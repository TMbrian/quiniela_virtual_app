package com.example.quiniela_virtual_app.presentation.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme

@Composable
fun AdminDashboardScreen(
    onAbrirPartidos: () -> Unit,
    onAbrirUsuarios: () -> Unit,
    onAbrirConfig: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
) {
    val statsEstado by viewModel.stats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Encabezado()
        when (val estado = statsEstado) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error   -> ErrorMessage(estado.mensaje)
            is UiState.Success -> ContenidoStats(estado.data)
        }
        HorizontalDivider()
        SeccionAcciones(onAbrirPartidos, onAbrirUsuarios, onAbrirConfig)
    }
}

@Composable
private fun Encabezado() {
    Column {
        Text(
            text = "Panel de administración",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Resumen en tiempo real",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ContenidoStats(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilaStatsPartidos(stats)
        FilaStatsUsuarios(stats)
        if (stats.lider != null) {
            LiderCard(stats.lider)
        }
    }
}

@Composable
private fun FilaStatsPartidos(stats: DashboardStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatChip(
            numero = "${stats.totalPartidos}",
            etiqueta = "partidos",
            modifier = Modifier.weight(1f),
        )
        StatChip(
            numero = "${stats.partidosEnVivo}",
            etiqueta = "en vivo",
            modifier = Modifier.weight(1f),
            destacado = stats.partidosEnVivo > 0,
        )
        StatChip(
            numero = "${stats.partidosFinalizados}",
            etiqueta = "finalizados",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FilaStatsUsuarios(stats: DashboardStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatChip(
            numero = "${stats.usuariosActivos}/${stats.totalUsuarios}",
            etiqueta = "usuarios activos",
            modifier = Modifier.weight(1f),
        )
        StatChip(
            numero = "${stats.totalPredicciones}",
            etiqueta = "predicciones",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatChip(
    numero: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    destacado: Boolean = false,
) {
    val containerColor = if (destacado) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceVariant
    val onContainerColor = if (destacado) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = numero,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onContainerColor,
            )
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = onContainerColor.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun LiderCard(lider: PosicionLeaderboard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MedallaOro()
            Column(Modifier.weight(1f)) {
                Text(
                    text = lider.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${lider.aciertosExactos} exactos · ${lider.aciertosTendencia} tendencias",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${lider.puntosTotales}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun MedallaOro() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(color = Color(0xFFFF8C00), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "1",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun SeccionAcciones(
    onAbrirPartidos: () -> Unit,
    onAbrirUsuarios: () -> Unit,
    onAbrirConfig: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Secciones",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(4.dp))
        AdminCard(
            titulo = "Partidos",
            descripcion = "Sincronizar desde API, gestionar estados y resultados",
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

// ── Datos para previews ──────────────────────────────────────────────────────

private fun statsFake() = DashboardStats(
    totalPartidos       = 48,
    partidosProgramados = 10,
    partidosEnVivo      = 2,
    partidosFinalizados = 36,
    totalUsuarios       = 8,
    usuariosActivos     = 7,
    lider               = PosicionLeaderboard("u1", "Brian Tzuc", null, 24, 5, 7, 15, emptyMap()),
    totalPredicciones   = 312,
)

@Preview(showBackground = true, name = "Dashboard — con stats")
@Composable
private fun AdminDashboardConStatsPreview() {
    QuinielaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Encabezado()
            ContenidoStats(statsFake())
            HorizontalDivider()
            SeccionAcciones(onAbrirPartidos = {}, onAbrirUsuarios = {}, onAbrirConfig = {})
        }
    }
}

@Preview(showBackground = true, name = "Dashboard — en vivo destacado")
@Composable
private fun StatChipEnVivoPreview() {
    QuinielaTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatChip("48", "partidos", Modifier.weight(1f))
            StatChip("2", "en vivo", Modifier.weight(1f), destacado = true)
            StatChip("36", "finalizados", Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true, name = "Dashboard — tarjeta líder")
@Composable
private fun LiderCardPreview() {
    QuinielaTheme {
        LiderCard(PosicionLeaderboard("u1", "Brian Tzuc", null, 24, 5, 7, 15, emptyMap()))
    }
}
