package com.example.quiniela_virtual_app.presentation.perfil

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import java.time.Instant

@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    when (val estado = uiState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error   -> ErrorMessage(estado.mensaje)
        is UiState.Success -> PerfilContent(estado.data, onCerrarSesion)
    }
}

@Composable
private fun PerfilContent(data: PerfilData, onCerrarSesion: () -> Unit) {
    val cerrarSesionColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        AvatarUsuario(nombre = data.usuario.nombre, fotoUrl = data.usuario.fotoUrl)
        Spacer(Modifier.height(16.dp))
        Text(data.usuario.nombre, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(data.usuario.email, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (data.usuario.esAdmin) "Administrador" else "Usuario",
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(24.dp))
        SeccionEstadisticas(data.puesto, data.stats)
        if (data.rachaActual > 0) {
            Spacer(Modifier.height(16.dp))
            RachaCard(data.rachaActual)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth(),
            colors = cerrarSesionColors,
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun SeccionEstadisticas(puesto: Int?, stats: PosicionLeaderboard?) {
    if (stats == null) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Mis estadísticas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                if (puesto != null) StatItem("Posición", "#$puesto")
                StatItem("Puntos", "${stats.puntosTotales}")
                StatItem("Jugados", "${stats.prediccionesTotales}")
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                val pctExacto = pct(stats.aciertosExactos, stats.prediccionesTotales)
                val pctTendencia = pct(stats.aciertosTendencia, stats.prediccionesTotales)
                StatItem("Exactos", "${stats.aciertosExactos}", "$pctExacto%")
                StatItem("Tendencias", "${stats.aciertosTendencia}", "$pctTendencia%")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, valor: String, detalle: String? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = valor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        if (detalle != null) {
            Text(
                text = detalle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun RachaCard(racha: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Racha actual",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Aciertos consecutivos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Text(
                text = "$racha",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun pct(valor: Int, total: Int): Int = if (total > 0) valor * 100 / total else 0

@Composable
private fun AvatarUsuario(nombre: String, fotoUrl: String?) {
    val iniciales = nombre.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
    SubcomposeAsyncImage(
        model = fotoUrl,
        contentDescription = "Foto de perfil",
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
        },
        error = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = iniciales,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    )
}

// ── Datos para previews ──────────────────────────────────────────────────────

private fun usuarioFake(esAdmin: Boolean) = Usuario(
    uid = "u1",
    nombre = "Brian Tzuc",
    email = "brian@ejemplo.com",
    fotoUrl = null,
    esAdmin = esAdmin,
    activo = true,
    creadoEn = Instant.now(),
    ultimoAcceso = Instant.now(),
)

private fun perfilDataFake(esAdmin: Boolean = false, conStats: Boolean = true) = PerfilData(
    usuario = usuarioFake(esAdmin),
    puesto = if (conStats) 3 else null,
    stats = if (conStats) PosicionLeaderboard("u1", "Brian Tzuc", null, 18, 3, 6, 15, emptyMap()) else null,
)

@Preview(showBackground = true, name = "Perfil — con estadísticas")
@Composable
private fun PerfilConStatsPreview() {
    QuinielaTheme {
        PerfilContent(data = perfilDataFake(), onCerrarSesion = {})
    }
}

@Preview(showBackground = true, name = "Perfil — sin estadísticas aún")
@Composable
private fun PerfilSinStatsPreview() {
    QuinielaTheme {
        PerfilContent(data = perfilDataFake(conStats = false), onCerrarSesion = {})
    }
}

@Preview(showBackground = true, name = "Perfil — administrador")
@Composable
private fun PerfilAdminPreview() {
    QuinielaTheme {
        PerfilContent(data = perfilDataFake(esAdmin = true), onCerrarSesion = {})
    }
}
