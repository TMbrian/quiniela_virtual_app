package com.example.quiniela_virtual_app.presentation.historial

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.presentation.partidos.PartidoConPrediccion
import com.example.quiniela_virtual_app.presentation.partidos.PartidoGrupoSeccion
import com.example.quiniela_virtual_app.presentation.partidos.PartidoJornadaSeccion
import com.example.quiniela_virtual_app.presentation.partidos.PartidosViewModel
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(viewModel: PartidosViewModel = hiltViewModel()) {
    val secciones by viewModel.secciones.collectAsState()
    val filtroJornada by viewModel.filtroJornada.collectAsState()
    val jornadasDisponibles by viewModel.jornadasDisponibles.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    Column(Modifier.fillMaxSize()) {
        if (jornadasDisponibles.size > 1) {
            JornadaFiltroChips(
                filtroJornada = filtroJornada,
                onJornadaChange = viewModel::filtrarJornada,
                jornadasDisponibles = jornadasDisponibles,
            )
            HorizontalDivider()
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::reiniciar,
            modifier = Modifier.weight(1f),
        ) {
            when (val estado = secciones) {
                is UiState.Loading -> LoadingIndicator()
                is UiState.Error   -> ErrorMessage(estado.mensaje)
                is UiState.Success -> ContenidoHistorial(filtrarHistorial(estado.data))
            }
        }
    }
}

@Composable
private fun JornadaFiltroChips(
    filtroJornada: Int?,
    onJornadaChange: (Int?) -> Unit,
    jornadasDisponibles: List<Int>,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "todas") {
            FilterChip(
                selected = filtroJornada == null,
                onClick = { onJornadaChange(null) },
                label = { Text("Todas") },
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

private fun filtrarHistorial(secciones: List<PartidoJornadaSeccion>): List<PartidoJornadaSeccion> =
    secciones.reversed().mapNotNull { seccion ->
        val gruposFiltrados = seccion.grupos.mapNotNull { grupo ->
            val historial = grupo.items.filter {
                it.partido.estado == EstadoPartido.FINALIZADO && it.prediccion != null
            }
            if (historial.isEmpty()) null else PartidoGrupoSeccion(grupo.nombre, historial)
        }
        if (gruposFiltrados.isEmpty()) null
        else PartidoJornadaSeccion(seccion.titulo, seccion.subtitulo, gruposFiltrados)
    }

@Composable
private fun ContenidoHistorial(secciones: List<PartidoJornadaSeccion>) {
    if (secciones.isEmpty()) {
        EstadoVacioHistorial()
        return
    }
    val todosItems = secciones.flatMap { s -> s.grupos.flatMap { it.items } }
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "resumen_global") {
            ResumenGlobalCard(todosItems)
        }
        secciones.forEach { seccion ->
            val itemsSeccion = seccion.grupos.flatMap { it.items }
            stickyHeader(key = "hdr_${seccion.titulo}") {
                JornadaHistorialHeader(titulo = seccion.titulo, items = itemsSeccion)
            }
            items(itemsSeccion, key = { it.partido.id }) { item ->
                TarjetaHistorial(item)
            }
        }
    }
}

@Composable
private fun ResumenGlobalCard(items: List<PartidoConPrediccion>) {
    val tipos      = items.map { tipoAcierto(it) }
    val exactos    = tipos.count { it == TipoAcierto.EXACTO }
    val tendencias = tipos.count { it == TipoAcierto.TENDENCIA }
    val fallos     = tipos.count { it == TipoAcierto.FALLO || it == TipoAcierto.SIN_DATOS }
    val puntos     = items.sumOf { it.puntosGanados ?: 0 }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Mi historial",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${items.size} partidos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                StatHistorial(exactos,    "Exactos",    MaterialTheme.colorScheme.primary)
                StatHistorial(tendencias, "Tendencias", MaterialTheme.colorScheme.secondary)
                StatHistorial(fallos,     "Fallos",     MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Puntos totales",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = "$puntos pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun StatHistorial(valor: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$valor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun JornadaHistorialHeader(titulo: String, items: List<PartidoConPrediccion>) {
    val puntosJornada = items.sumOf { it.puntosGanados ?: 0 }
    val exactosJornada = items.count { tipoAcierto(it) == TipoAcierto.EXACTO }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$exactosJornada exactos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                )
                Text(
                    text = "+$puntosJornada pts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun TarjetaHistorial(item: PartidoConPrediccion) {
    val prediccion = item.prediccion ?: return
    val tipo = tipoAcierto(item)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            FilaEquiposHistorial(item.partido)
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.partido.fecha.toFechaCorta(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            FilaComparacion(prediccion, tipo, item.puntosGanados)
        }
    }
}

@Composable
private fun FilaEquiposHistorial(partido: Partido) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        EquipoColumnHistorial(
            nombre   = partido.equipoLocal.nombre,
            crestUrl = partido.equipoLocal.crestUrl,
            iso      = partido.equipoLocal.iso,
            modifier = Modifier.weight(1f),
        )
        MarcadorFinal(partido.golesLocal, partido.golesVisitante)
        EquipoColumnHistorial(
            nombre   = partido.equipoVisitante.nombre,
            crestUrl = partido.equipoVisitante.crestUrl,
            iso      = partido.equipoVisitante.iso,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EquipoColumnHistorial(
    nombre: String,
    crestUrl: String?,
    iso: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SubcomposeAsyncImage(
            model = crestUrl,
            contentDescription = nombre,
            modifier = Modifier.size(36.dp),
            contentScale = ContentScale.Fit,
            loading = {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            },
            error = {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = iso.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = nombre,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MarcadorFinal(golesLocal: Int?, golesVisitante: Int?) {
    val texto = if (golesLocal != null && golesVisitante != null)
        "$golesLocal - $golesVisitante" else "? - ?"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Final",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun FilaComparacion(prediccion: Prediccion, tipo: TipoAcierto, puntosGanados: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Tu predicción",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = "${prediccion.golesLocal} - ${prediccion.golesVisitante}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        AciertoChip(tipo)
        Text(
            text = "+${puntosGanados ?: 0} pts",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colorPuntos(tipo),
        )
    }
}

@Composable
private fun AciertoChip(tipo: TipoAcierto) {
    val containerColor = when (tipo) {
        TipoAcierto.EXACTO    -> MaterialTheme.colorScheme.primaryContainer
        TipoAcierto.TENDENCIA -> MaterialTheme.colorScheme.secondaryContainer
        TipoAcierto.FALLO     -> MaterialTheme.colorScheme.errorContainer
        TipoAcierto.SIN_DATOS -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (tipo) {
        TipoAcierto.EXACTO    -> MaterialTheme.colorScheme.onPrimaryContainer
        TipoAcierto.TENDENCIA -> MaterialTheme.colorScheme.onSecondaryContainer
        TipoAcierto.FALLO     -> MaterialTheme.colorScheme.onErrorContainer
        TipoAcierto.SIN_DATOS -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val texto = when (tipo) {
        TipoAcierto.EXACTO    -> "Exacto ✓"
        TipoAcierto.TENDENCIA -> "Tendencia ✓"
        TipoAcierto.FALLO     -> "Fallo ✗"
        TipoAcierto.SIN_DATOS -> "Sin datos"
    }
    Surface(color = containerColor, shape = RoundedCornerShape(50)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun colorPuntos(tipo: TipoAcierto): Color = when (tipo) {
    TipoAcierto.EXACTO,
    TipoAcierto.TENDENCIA -> MaterialTheme.colorScheme.primary
    TipoAcierto.FALLO,
    TipoAcierto.SIN_DATOS -> MaterialTheme.colorScheme.outline
}

@Composable
private fun EstadoVacioHistorial() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Sin historial aún",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Aquí verás tus predicciones y resultados cuando finalicen los partidos.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private enum class TipoAcierto { EXACTO, TENDENCIA, FALLO, SIN_DATOS }

private fun tipoAcierto(item: PartidoConPrediccion): TipoAcierto {
    val pred = item.prediccion ?: return TipoAcierto.SIN_DATOS
    val gL   = item.partido.golesLocal ?: return TipoAcierto.SIN_DATOS
    val gV   = item.partido.golesVisitante ?: return TipoAcierto.SIN_DATOS
    if (gL == pred.golesLocal && gV == pred.golesVisitante) return TipoAcierto.EXACTO
    return if (mismoGanadorH(gL, gV, pred.golesLocal, pred.golesVisitante)) TipoAcierto.TENDENCIA else TipoAcierto.FALLO
}

private fun mismoGanadorH(gL: Int, gV: Int, pL: Int, pV: Int): Boolean =
    (gL > gV && pL > pV) || (gL < gV && pL < pV) || (gL == gV && pL == pV)

private val fechaFormatterH = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-MX"))

private fun Instant.toFechaCorta(): String = atZone(ZoneId.systemDefault()).format(fechaFormatterH)
