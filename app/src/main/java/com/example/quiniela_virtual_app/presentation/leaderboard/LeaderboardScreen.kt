package com.example.quiniela_virtual_app.presentation.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun LeaderboardScreen(viewModel: LeaderboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    when (val estado = uiState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error   -> ErrorMessage(estado.mensaje)
        is UiState.Success -> LeaderboardList(estado.data)
    }
}

@Composable
private fun LeaderboardList(posiciones: List<PosicionLeaderboard>) {
    if (posiciones.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aún no hay posiciones registradas.")
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp, vertical = 8.dp,
        ),
    ) {
        itemsIndexed(posiciones, key = { _, p -> p.uid }) { index, posicion ->
            PosicionCard(posicion = posicion, numero = index + 1)
        }
    }
}

@Composable
private fun PosicionCard(posicion: PosicionLeaderboard, numero: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (numero <= 3) 4.dp else 1.dp),
        colors = if (numero == 1) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                 else CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PosicionBadge(numero = numero)
            Column(Modifier.weight(1f)) {
                Text(
                    text = posicion.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = statsTexto(posicion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${posicion.puntosTotales}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (numero == 1) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
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
private fun PosicionBadge(numero: Int) {
    val (fondo, texto) = badgeColores(numero)
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(color = fondo, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$numero",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = texto,
        )
    }
}

private fun badgeColores(numero: Int): Pair<Color, Color> = when (numero) {
    1    -> Color(0xFFFF8C00) to Color.White
    2    -> Color(0xFFC0C0C0) to Color(0xFF37474F)
    3    -> Color(0xFFCD7F32) to Color.White
    else -> Color(0xFFE0E0E0) to Color(0xFF424242)
}

private fun statsTexto(p: PosicionLeaderboard): String =
    "${p.aciertosExactos} exactos · ${p.aciertosTendencia} tendencias · ${p.prediccionesTotales} jugados"

// ── Datos para previews ──────────────────────────────────────────────────────

private fun posicionesFake() = listOf(
    PosicionLeaderboard("u1", "Brian Tzuc",    null, 24, 5, 7, 15, emptyMap()),
    PosicionLeaderboard("u2", "Ana García",    null, 18, 3, 6, 14, emptyMap()),
    PosicionLeaderboard("u3", "Carlos López",  null, 15, 2, 5, 13, emptyMap()),
    PosicionLeaderboard("u4", "María Sánchez", null, 12, 1, 5, 12, emptyMap()),
    PosicionLeaderboard("u5", "José Ramírez",  null, 9,  1, 3, 11, emptyMap()),
)

@Preview(showBackground = true, name = "Leaderboard")
@Composable
private fun LeaderboardPreview() {
    QuinielaTheme {
        LeaderboardList(posicionesFake())
    }
}

@Preview(showBackground = true, name = "Leaderboard — posición #1")
@Composable
private fun PosicionCardOroPreview() {
    QuinielaTheme {
        PosicionCard(
            posicion = PosicionLeaderboard("u1", "Brian Tzuc", null, 24, 5, 7, 15, emptyMap()),
            numero = 1,
        )
    }
}
