package com.example.quiniela_virtual_app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.quiniela_virtual_app.BuildConfig
import com.example.quiniela_virtual_app.presentation.admin.config.AdminConfigScreen
import com.example.quiniela_virtual_app.presentation.admin.dashboard.AdminDashboardScreen
import com.example.quiniela_virtual_app.presentation.admin.partidos.AdminPartidosScreen
import com.example.quiniela_virtual_app.presentation.admin.usuarios.AdminUsuariosScreen
import com.example.quiniela_virtual_app.presentation.auth.LoginScreen
import com.example.quiniela_virtual_app.presentation.auth.LoginViewModel
import com.example.quiniela_virtual_app.presentation.leaderboard.LeaderboardScreen
import com.example.quiniela_virtual_app.presentation.partidos.PartidosScreen
import com.example.quiniela_virtual_app.presentation.perfil.PerfilScreen

@Composable
fun QuinielaNavGraph() {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val estaAutenticado by loginViewModel.estaAutenticado.collectAsState()
    val usuarioActual by loginViewModel.usuarioActual.collectAsState()
    val esAdmin = usuarioActual?.esAdmin == true && BuildConfig.IS_ADMIN_BUILD
    val estaActivo = usuarioActual?.activo != false

    when {
        !estaAutenticado -> LoginScreen(viewModel = loginViewModel)
        !estaActivo      -> CuentaDeshabilitadaScreen { loginViewModel.cerrarSesion() }
        else             -> AppPrincipal(loginViewModel, esAdmin)
    }
}

@Composable
private fun AppPrincipal(loginViewModel: LoginViewModel, esAdmin: Boolean) {
    val navController = rememberNavController()
    Scaffold(bottomBar = { BarraNavegacion(navController, esAdmin) }) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Partidos.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Screen.Partidos.route)    { PartidosScreen() }
            composable(Screen.Leaderboard.route) { LeaderboardScreen() }
            composable(Screen.Perfil.route)      {
                PerfilScreen(onCerrarSesion = { loginViewModel.cerrarSesion() })
            }
            if (BuildConfig.IS_ADMIN_BUILD) {
                composable(Screen.AdminDashboard.route) {
                    AdminDashboardScreen(
                        onAbrirPartidos = { navController.navigate(Screen.AdminPartidos.route) },
                        onAbrirUsuarios = { navController.navigate(Screen.AdminUsuarios.route) },
                        onAbrirConfig   = { navController.navigate(Screen.AdminConfig.route) },
                    )
                }
                composable(Screen.AdminPartidos.route) { AdminPartidosScreen() }
                composable(Screen.AdminUsuarios.route) { AdminUsuariosScreen() }
                composable(Screen.AdminConfig.route)   { AdminConfigScreen() }
            }
        }
    }
}

@Composable
private fun BarraNavegacion(navController: NavController, esAdmin: Boolean) {
    val backStack by navController.currentBackStackEntryAsState()
    val rutaActual = backStack?.destination?.route
    NavigationBar {
        itemsNav(esAdmin).forEach { item ->
            NavigationBarItem(
                selected = rutaActual == item.pantalla.route,
                onClick = { navegarA(navController, item.pantalla) },
                icon = { Icon(item.icono, contentDescription = item.etiqueta) },
                label = { Text(item.etiqueta) },
            )
        }
    }
}

private fun navegarA(navController: NavController, pantalla: Screen) {
    navController.navigate(pantalla.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun itemsNav(esAdmin: Boolean) = buildList {
    add(NavItem(Screen.Partidos, "Partidos", Icons.Default.Home))
    add(NavItem(Screen.Leaderboard, "Posiciones", Icons.Default.Star))
    add(NavItem(Screen.Perfil, "Perfil", Icons.Default.Person))
    if (esAdmin) add(NavItem(Screen.AdminDashboard, "Admin", Icons.Default.Settings))
}

private data class NavItem(
    val pantalla: Screen,
    val etiqueta: String,
    val icono: ImageVector,
)

@Composable
private fun CuentaDeshabilitadaScreen(onCerrarSesion: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tu cuenta está deshabilitada.")
            Spacer(Modifier.height(4.dp))
            Text("Contacta al administrador.")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onCerrarSesion,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text("Cerrar sesión") }
        }
    }
}
