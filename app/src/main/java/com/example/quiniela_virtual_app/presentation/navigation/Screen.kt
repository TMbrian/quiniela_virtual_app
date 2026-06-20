package com.example.quiniela_virtual_app.presentation.navigation

sealed class Screen(val route: String) {
    data object Login          : Screen("login")
    data object Partidos       : Screen("partidos")
    data object Leaderboard    : Screen("leaderboard")
    data object Perfil         : Screen("perfil")
    data object AdminDashboard : Screen("admin/dashboard")
    data object AdminPartidos  : Screen("admin/partidos")
    data object AdminUsuarios  : Screen("admin/usuarios")
    data object AdminConfig    : Screen("admin/config")
}
