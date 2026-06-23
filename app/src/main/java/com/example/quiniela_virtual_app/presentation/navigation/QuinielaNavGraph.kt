package com.example.quiniela_virtual_app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.quiniela_virtual_app.presentation.grupos.GruposScreen
import com.example.quiniela_virtual_app.presentation.historial.HistorialScreen
import com.example.quiniela_virtual_app.presentation.leaderboard.LeaderboardScreen
import com.example.quiniela_virtual_app.presentation.partidos.PartidosScreen
import com.example.quiniela_virtual_app.presentation.perfil.PerfilScreen
import com.example.quiniela_virtual_app.presentation.predicciones.PrediccionesScreen
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import kotlinx.coroutines.launch

private val RUTAS_DETALLE = setOf(
    Screen.AdminPartidos.route,
    Screen.AdminUsuarios.route,
    Screen.AdminConfig.route,
)

@Composable
fun QuinielaNavGraph() {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val estaAutenticado by loginViewModel.estaAutenticado.collectAsState()
    val usuarioActual by loginViewModel.usuarioActual.collectAsState()
    val esAdminFirestore = usuarioActual?.esAdmin == true
    val esAdmin = esAdminFirestore && BuildConfig.IS_ADMIN_BUILD
    val estaActivo = usuarioActual?.activo != false

    when {
        !estaAutenticado ->
            LoginScreen(viewModel = loginViewModel)
        usuarioActual == null ->
            LoadingIndicator()
        !estaActivo ->
            CuentaDeshabilitadaScreen { loginViewModel.cerrarSesion() }
        BuildConfig.IS_ADMIN_BUILD && !esAdminFirestore ->
            AccesoRestringidoScreen("Esta versión es exclusiva para administradores.") { loginViewModel.cerrarSesion() }
        !BuildConfig.IS_ADMIN_BUILD && esAdminFirestore ->
            AccesoRestringidoScreen("Esta versión es para participantes.\nUsa la app de administración.") { loginViewModel.cerrarSesion() }
        else ->
            AppPrincipal(loginViewModel, esAdmin)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPrincipal(loginViewModel: LoginViewModel, esAdmin: Boolean) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val rutaActual = backStack?.destination?.route
    val esDetalle = rutaActual in RUTAS_DETALLE

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                navController = navController,
                esAdmin = esAdmin,
                rutaActual = rutaActual,
                onCerrar = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    titulo = tituloRuta(rutaActual),
                    esDetalle = esDetalle,
                    onAbrirMenu = { scope.launch { drawerState.open() } },
                    onVolver = { navController.popBackStack() },
                )
            },
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Predicciones.route,
                modifier = Modifier.padding(paddingValues),
            ) {
                composable(Screen.Predicciones.route) {
                    PrediccionesScreen(onVerPartido = { navegarA(navController, Screen.Partidos) })
                }
                composable(Screen.Partidos.route)    { PartidosScreen() }
                composable(Screen.Grupos.route)      { GruposScreen() }
                composable(Screen.Historial.route)   { HistorialScreen() }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    titulo: String,
    esDetalle: Boolean,
    onAbrirMenu: () -> Unit,
    onVolver: () -> Unit,
) {
    TopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            if (esDetalle) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            } else {
                IconButton(onClick = onAbrirMenu) {
                    Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                }
            }
        },
    )
}

@Composable
private fun DrawerContent(
    navController: NavController,
    esAdmin: Boolean,
    rutaActual: String?,
    onCerrar: () -> Unit,
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Quiniela Virtual",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider(Modifier.padding(bottom = 8.dp))
        itemsNav(esAdmin).forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.etiqueta) },
                selected = rutaActual == item.pantalla.route,
                onClick = {
                    navegarA(navController, item.pantalla)
                    onCerrar()
                },
                icon = { Icon(item.icono, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

private fun tituloRuta(ruta: String?): String = when (ruta) {
    Screen.Predicciones.route   -> "Mis predicciones"
    Screen.Partidos.route       -> "Partidos"
    Screen.Grupos.route         -> "Tablas de grupo"
    Screen.Historial.route      -> "Historial"
    Screen.Leaderboard.route    -> "Posiciones"
    Screen.Perfil.route         -> "Perfil"
    Screen.AdminDashboard.route -> "Panel admin"
    Screen.AdminPartidos.route  -> "Gestión de partidos"
    Screen.AdminUsuarios.route  -> "Usuarios"
    Screen.AdminConfig.route    -> "Configuración"
    else                        -> "Quiniela"
}

private fun navegarA(navController: NavController, pantalla: Screen) {
    navController.navigate(pantalla.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun itemsNav(esAdmin: Boolean) = buildList {
    add(NavItem(Screen.Predicciones, "Predecir",  Icons.Default.Edit))
    add(NavItem(Screen.Partidos,     "Partidos",  Icons.Default.Home))
    add(NavItem(Screen.Grupos,       "Grupos",    Icons.Default.List))
    add(NavItem(Screen.Historial,    "Historial", Icons.Default.DateRange))
    add(NavItem(Screen.Leaderboard,  "Posiciones", Icons.Default.Star))
    add(NavItem(Screen.Perfil,       "Perfil",    Icons.Default.Person))
    if (esAdmin) add(NavItem(Screen.AdminDashboard, "Admin", Icons.Default.Settings))
}

private data class NavItem(
    val pantalla: Screen,
    val etiqueta: String,
    val icono: ImageVector,
)

@Composable
private fun AccesoRestringidoScreen(mensaje: String, onCerrarSesion: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
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
