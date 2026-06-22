package com.example.quiniela_virtual_app.presentation.leaderboard

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUid = viewModel.currentUid
    val filtroJornada by viewModel.filtroJornada.collectAsState()
    val jornadasDisponibles by viewModel.jornadasDisponibles.collectAsState()
    Column(Modifier.fillMaxSize()) {
        if (jornadasDisponibles.size > 1) {
            JornadaFiltroChips(
                filtroJornada = filtroJornada,
                onJornadaChange = viewModel::filtrarJornada,
                jornadasDisponibles = jornadasDisponibles,
            )
            HorizontalDivider()
        }
        when (val estado = uiState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error   -> ErrorMessage(estado.mensaje)
            is UiState.Success -> LeaderboardList(
                posiciones = estado.data,
                currentUid = currentUid,
                filtroJornada = filtroJornada,
            )
        }
    }
}

@Composable
private fun LeaderboardList(posiciones: List<PosicionLeaderboard>, currentUid: String?, filtroJornada: String? = null) {
    if (posiciones.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aún no hay posiciones registradas.")
        }
        return
    }
    val miIndice = posiciones.indexOfFirst { it.uid == currentUid }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(miIndice) {
        if (miIndice >= 0) listState.animateScrollToItem(miIndice)
    }

    val miPosicionVisible by remember {
        derivedStateOf {
            if (miIndice < 0) return@derivedStateOf true
            listState.layoutInfo.visibleItemsInfo.any { it.index == miIndice }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            itemsIndexed(posiciones, key = { _, p -> p.uid }) { index, posicion ->
                PosicionCard(
                    posicion = posicion,
                    numero = index + 1,
                    esMia = posicion.uid == currentUid,
                    filtroJornada = filtroJornada,
                )
            }
        }
        AnimatedVisibility(
            visible = !miPosicionVisible,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(miIndice) } },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Mi posición",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PosicionCard(posicion: PosicionLeaderboard, numero: Int, esMia: Boolean = false, filtroJornada: String? = null) {
    val context = LocalContext.current
    val cardColors = if (numero == 1) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                     else CardDefaults.cardColors()
    val puntosColor = if (numero == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val borde = if (esMia && numero != 1) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    val puntosDisplay = if (filtroJornada != null) posicion.puntosPorJornada[filtroJornada] ?: 0 else posicion.puntosTotales
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (numero <= 3) 4.dp else 1.dp),
        colors = cardColors,
        border = borde,
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
                    text = statsTexto(posicion, filtroJornada),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$puntosDisplay",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = puntosColor,
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (esMia) {
                IconButton(onClick = { compartirPosicion(context, numero, posicion) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir posición",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun compartirPosicion(context: android.content.Context, numero: Int, posicion: PosicionLeaderboard) {
    val texto = "Voy #$numero con ${posicion.puntosTotales} pts en la quiniela 🏆"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, null))
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

private fun statsTexto(p: PosicionLeaderboard, filtroJornada: String?): String {
    if (filtroJornada != null) return "Jornada $filtroJornada · ${p.puntosPorJornada[filtroJornada] ?: 0} pts"
    return "${p.aciertosExactos} exactos · ${p.aciertosTendencia} tendencias · ${p.prediccionesTotales} jugados"
}

@Composable
private fun JornadaFiltroChips(
    filtroJornada: String?,
    onJornadaChange: (String?) -> Unit,
    jornadasDisponibles: List<String>,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "todos") {
            FilterChip(
                selected = filtroJornada == null,
                onClick = { onJornadaChange(null) },
                label = { Text("Todos") },
            )
        }
        items(jornadasDisponibles, key = { "j_$it" }) { jornada ->
            FilterChip(
                selected = filtroJornada == jornada,
                onClick = { onJornadaChange(jornada) },
                label = { Text("J$jornada") },
            )
        }
    }
}

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
        LeaderboardList(posiciones = posicionesFake(), currentUid = "u2")
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
