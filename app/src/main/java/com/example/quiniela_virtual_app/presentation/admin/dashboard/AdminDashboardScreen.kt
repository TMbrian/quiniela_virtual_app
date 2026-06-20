package com.example.quiniela_virtual_app.presentation.admin.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.tooling.preview.Preview
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminDashboardScreen(
    onAbrirPartidos: () -> Unit,
    onAbrirUsuarios: () -> Unit,
    onAbrirConfig: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Text("Panel de administración", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        AdminCard(
            titulo = "Partidos",
            descripcion = "Sincronizar desde API, actualizar resultados",
            onClick = onAbrirPartidos,
        )
        AdminCard(
            titulo = "Usuarios",
            descripcion = "Gestionar roles y estado de los participantes",
            onClick = onAbrirUsuarios,
        )
        AdminCard(
            titulo = "Configuración",
            descripcion = "Puntos, bloqueo de predicciones y nombre de la competición",
            onClick = onAbrirConfig,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminCard(titulo: String, descripcion: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(descripcion, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true, name = "Admin — dashboard")
@Composable
private fun AdminDashboardPreview() {
    QuinielaTheme {
        AdminDashboardScreen(
            onAbrirPartidos = {},
            onAbrirUsuarios = {},
            onAbrirConfig = {},
        )
    }
}
