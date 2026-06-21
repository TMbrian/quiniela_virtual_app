package com.example.quiniela_virtual_app.presentation.admin.partidos

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.EstadoBadge
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AdminPartidosScreen(viewModel: AdminPartidosViewModel = hiltViewModel()) {
    val secciones by viewModel.secciones.collectAsState()
    val cargando by viewModel.cargandoAccion.collectAsState()
    val mensaje by viewModel.accionEstado.collectAsState()
    val filtro by viewModel.filtroEstado.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let { snackbarHost.showSnackbar(it); viewModel.limpiarMensaje() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AccionesGlobales(
                cargando = cargando,
                onSincronizar = viewModel::sincronizarDesdeApi,
                onRestablecer = viewModel::restablecer,
            )
            HorizontalDivider()
            FiltrosChips(filtroActual = filtro, onFiltroChange = viewModel::filtrarPor)
            HorizontalDivider()
            when (val estado = secciones) {
                is UiState.Loading -> LoadingIndicator()
                is UiState.Error   -> ErrorMessage(estado.mensaje)
                is UiState.Success -> PartidosPorSecciones(
                    secciones = estado.data,
                    onActualizar = viewModel::actualizarResultado,
                    onCambiarEstado = viewModel::cambiarEstado,
                    onToggleExcluido = viewModel::toggleExcluido,
                    onAbrirPredicciones = viewModel::abrirPredicciones,
                    onForzarBloqueo = viewModel::forzarBloqueo,
                )
            }
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun AccionesGlobales(
    cargando: Boolean,
    onSincronizar: () -> Unit,
    onRestablecer: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onSincronizar, enabled = !cargando, modifier = Modifier.weight(1f)) {
            if (cargando) CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
            else Text("Sincronizar API")
        }
        OutlinedButton(onClick = onRestablecer, enabled = !cargando, modifier = Modifier.weight(1f)) {
            Text("Restablecer")
        }
    }
}

private val FILTROS: List<Pair<EstadoPartido?, String>> = listOf(
    null to "Todos",
    EstadoPartido.EN_VIVO to "En vivo",
    EstadoPartido.PROGRAMADO to "Programados",
    EstadoPartido.FINALIZADO to "Finalizados",
)

@Composable
private fun FiltrosChips(
    filtroActual: EstadoPartido?,
    onFiltroChange: (EstadoPartido?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(FILTROS, key = { it.second }) { (estado, etiqueta) ->
            FilterChip(
                selected = filtroActual == estado,
                onClick = { onFiltroChange(estado) },
                label = { Text(etiqueta) },
            )
        }
    }
}

@Composable
private fun PartidosPorSecciones(
    secciones: List<JornadaSeccion>,
    onActualizar: (Partido, Int, Int) -> Unit,
    onCambiarEstado: (Partido, EstadoPartido) -> Unit,
    onToggleExcluido: (Partido) -> Unit,
    onAbrirPredicciones: (Partido, Long) -> Unit,
    onForzarBloqueo: (Partido) -> Unit,
) {
    if (secciones.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay partidos para mostrar.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        secciones.forEach { seccion ->
            stickyHeader(key = "jornada_${seccion.titulo}") {
                JornadaHeader(seccion)
            }
            seccion.grupos.forEach { grupo ->
                item(key = "grupo_${seccion.titulo}_${grupo.nombre}") {
                    GrupoSubHeader(grupo.nombre)
                }
                items(grupo.partidos, key = { it.id }) { partido ->
                    AdminPartidoCard(
                        partido = partido,
                        onActualizar = onActualizar,
                        onCambiarEstado = onCambiarEstado,
                        onToggleExcluido = onToggleExcluido,
                        onAbrirPredicciones = onAbrirPredicciones,
                        onForzarBloqueo = onForzarBloqueo,
                    )
                }
            }
        }
    }
}

@Composable
private fun JornadaHeader(seccion: JornadaSeccion) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = seccion.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (seccion.subtitulo.isNotBlank()) {
                Text(
                    text = seccion.subtitulo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun GrupoSubHeader(nombre: String) {
    if (nombre.isBlank() || nombre == "-") return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text = "Grupo $nombre",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun AdminPartidoCard(
    partido: Partido,
    onActualizar: (Partido, Int, Int) -> Unit,
    onCambiarEstado: (Partido, EstadoPartido) -> Unit,
    onToggleExcluido: (Partido) -> Unit,
    onAbrirPredicciones: (Partido, Long) -> Unit,
    onForzarBloqueo: (Partido) -> Unit,
) {
    var golesLocal by remember(partido.golesLocal) {
        mutableStateOf(partido.golesLocal?.toString() ?: "")
    }
    var golesVisitante by remember(partido.golesVisitante) {
        mutableStateOf(partido.golesVisitante?.toString() ?: "")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            CabeceraCard(partido = partido)
            Spacer(Modifier.height(10.dp))
            FilaEquiposYMarcador(partido = partido)
            Spacer(Modifier.height(6.dp))
            FechaYSede(partido = partido)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            SeccionEstado(partido = partido, onCambiarEstado = onCambiarEstado)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            FilaExcluido(partido = partido, onToggle = onToggleExcluido)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            SeccionLockOverride(
                partido = partido,
                onAbrirPredicciones = onAbrirPredicciones,
                onForzarBloqueo = onForzarBloqueo,
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            ActualizarMarcadorRow(
                golesLocal = golesLocal,
                golesVisitante = golesVisitante,
                partido = partido,
                onGolesLocalChange = { if (it.length <= 2) golesLocal = it },
                onGolesVisitanteChange = { if (it.length <= 2) golesVisitante = it },
                onActualizar = onActualizar,
            )
        }
    }
}

@Composable
private fun CabeceraCard(partido: Partido) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EstadoBadge(partido.estado)
        Spacer(Modifier.weight(1f))
        Text(
            text = if (partido.fase == "Grupos") "J${partido.jornada}" else partido.fase,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun FilaEquiposYMarcador(partido: Partido) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EquipoDisplay(equipo = partido.equipoLocal, modifier = Modifier.weight(1f))
        MarcadorDisplay(golesLocal = partido.golesLocal, golesVisitante = partido.golesVisitante)
        EquipoDisplay(equipo = partido.equipoVisitante, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EquipoDisplay(equipo: Equipo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = equipo.iso.take(3).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = equipo.nombre,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MarcadorDisplay(golesLocal: Int?, golesVisitante: Int?) {
    val texto = if (golesLocal != null && golesVisitante != null)
        "$golesLocal - $golesVisitante"
    else
        "vs"
    Text(
        text = texto,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun FechaYSede(partido: Partido) {
    Text(
        text = "${partido.fecha.toFechaCorta()}  ·  ${partido.sede}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SeccionEstado(
    partido: Partido,
    onCambiarEstado: (Partido, EstadoPartido) -> Unit,
) {
    val estados = EstadoPartido.entries
    Column {
        Text(
            text = "Estado",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            estados.forEachIndexed { index, estado ->
                SegmentedButton(
                    selected = estado == partido.estado,
                    onClick = { onCambiarEstado(partido, estado) },
                    shape = SegmentedButtonDefaults.itemShape(index, estados.size),
                    label = { Text(etiquetaEstado(estado)) },
                )
            }
        }
    }
}

@Composable
private fun FilaExcluido(partido: Partido, onToggle: (Partido) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Excluir partido",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "No computará en predicciones ni puntos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Switch(
            checked = partido.excluido,
            onCheckedChange = { onToggle(partido) },
        )
    }
}

private val DURACIONES_LOCK: List<Pair<Long, String>> = listOf(
    30 * 60_000L to "30 min",
    60 * 60_000L to "1 hora",
    2 * 60 * 60_000L to "2 horas",
)

@Composable
private fun SeccionLockOverride(
    partido: Partido,
    onAbrirPredicciones: (Partido, Long) -> Unit,
    onForzarBloqueo: (Partido) -> Unit,
) {
    var duracionIdx by remember { mutableStateOf(0) }

    Column {
        Text(
            text = "Ventana de predicción",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(4.dp))
        EstadoLockText(partido)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            DURACIONES_LOCK.forEachIndexed { index, (_, etiqueta) ->
                SegmentedButton(
                    selected = index == duracionIdx,
                    onClick = { duracionIdx = index },
                    shape = SegmentedButtonDefaults.itemShape(index, DURACIONES_LOCK.size),
                    label = { Text(etiqueta) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onAbrirPredicciones(partido, DURACIONES_LOCK[duracionIdx].first) },
                modifier = Modifier.weight(1f),
            ) { Text("Abrir predicciones") }
            OutlinedButton(
                onClick = { onForzarBloqueo(partido) },
                modifier = Modifier.weight(1f),
            ) { Text("Forzar cierre") }
        }
    }
}

@Composable
private fun EstadoLockText(partido: Partido) {
    val ahora = System.currentTimeMillis()
    val expiry = partido.lockOverrideExpiry
    val (texto, color) = when {
        partido.lockOverride == true ->
            "Forzado cerrado por admin" to MaterialTheme.colorScheme.error
        partido.lockOverride == false && expiry != null && ahora < expiry.toEpochMilli() ->
            "Abierto hasta ${expiry.toFechaCorta()}" to MaterialTheme.colorScheme.primary
        partido.lockOverride == false ->
            "Ventana expirada" to MaterialTheme.colorScheme.outline
        else ->
            "Automático según horario" to MaterialTheme.colorScheme.outline
    }
    Text(text = texto, style = MaterialTheme.typography.labelSmall, color = color)
}

private fun etiquetaEstado(estado: EstadoPartido): String = when (estado) {
    EstadoPartido.PROGRAMADO -> "Prog"
    EstadoPartido.EN_VIVO    -> "Vivo"
    EstadoPartido.FINALIZADO -> "Final"
    EstadoPartido.SUSPENDIDO -> "Susp"
}

@Composable
private fun ActualizarMarcadorRow(
    golesLocal: String,
    golesVisitante: String,
    partido: Partido,
    onGolesLocalChange: (String) -> Unit,
    onGolesVisitanteChange: (String) -> Unit,
    onActualizar: (Partido, Int, Int) -> Unit,
) {
    val hayCambios = golesLocal != (partido.golesLocal?.toString() ?: "") ||
                     golesVisitante != (partido.golesVisitante?.toString() ?: "")
    val inputValido = golesLocal.toIntOrNull() != null && golesVisitante.toIntOrNull() != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = golesLocal,
            onValueChange = onGolesLocalChange,
            modifier = Modifier.width(64.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("L") },
        )
        Text(
            text = "-",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = golesVisitante,
            onValueChange = onGolesVisitanteChange,
            modifier = Modifier.width(64.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("V") },
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val gL = golesLocal.toIntOrNull() ?: return@Button
                val gV = golesVisitante.toIntOrNull() ?: return@Button
                onActualizar(partido, gL, gV)
            },
            enabled = hayCambios && inputValido,
        ) {
            Text("Actualizar")
        }
    }
}

private val fechaFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-MX"))

private fun Instant.toFechaCorta(): String =
    atZone(ZoneId.systemDefault()).format(fechaFormatter)

// ── Datos para previews ──────────────────────────────────────────────────────

private fun partidoFake(
    id: String = "p1",
    local: String = "México",
    visitante: String = "Argentina",
    golesLocal: Int? = null,
    golesVisitante: Int? = null,
    estado: EstadoPartido = EstadoPartido.PROGRAMADO,
    excluido: Boolean = false,
) = Partido(
    id = id, jornada = 1, grupo = "A", fase = "Grupos",
    equipoLocal = Equipo(local, local.take(3).uppercase()),
    equipoVisitante = Equipo(visitante, visitante.take(3).uppercase()),
    fecha = Instant.now(), sede = "Estadio Azteca",
    golesLocal = golesLocal, golesVisitante = golesVisitante,
    estado = estado, excluido = excluido,
)

private fun seccionesFake() = listOf(
    JornadaSeccion(
        titulo = "Jornada 1", subtitulo = "Grupos",
        grupos = listOf(
            GrupoSeccion(
                nombre = "A",
                partidos = listOf(
                    partidoFake("p1"),
                    partidoFake("p2", local = "Brasil", visitante = "Francia", golesLocal = 1, golesVisitante = 0, estado = EstadoPartido.FINALIZADO),
                ),
            ),
            GrupoSeccion(
                nombre = "B",
                partidos = listOf(
                    partidoFake("p3", local = "España", visitante = "Alemania", estado = EstadoPartido.EN_VIVO),
                ),
            ),
        ),
    ),
    JornadaSeccion(
        titulo = "Octavos", subtitulo = "",
        grupos = listOf(
            GrupoSeccion(
                nombre = "",
                partidos = listOf(
                    partidoFake("p4", local = "Argentina", visitante = "Brasil"),
                ),
            ),
        ),
    ),
)

@Preview(showBackground = true, name = "Admin — pantalla completa")
@Composable
private fun AdminPartidosScreenPreview() {
    QuinielaTheme {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                AccionesGlobales(cargando = false, onSincronizar = {}, onRestablecer = {})
                HorizontalDivider()
                FiltrosChips(filtroActual = null, onFiltroChange = {})
                HorizontalDivider()
                PartidosPorSecciones(
                    secciones = seccionesFake(),
                    onActualizar = { _, _, _ -> },
                    onCambiarEstado = { _, _ -> },
                    onToggleExcluido = {},
                    onAbrirPredicciones = { _, _ -> },
                    onForzarBloqueo = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Admin — tarjeta finalizado")
@Composable
private fun AdminPartidoCardFinalizadoPreview() {
    QuinielaTheme {
        AdminPartidoCard(
            partido = partidoFake("p2", local = "Brasil", visitante = "Francia", golesLocal = 1, golesVisitante = 0, estado = EstadoPartido.FINALIZADO),
            onActualizar = { _, _, _ -> },
            onCambiarEstado = { _, _ -> },
            onToggleExcluido = {},
            onAbrirPredicciones = { _, _ -> },
            onForzarBloqueo = {},
        )
    }
}

@Preview(showBackground = true, name = "Admin — tarjeta en vivo excluido")
@Composable
private fun AdminPartidoCardEnVivoPreview() {
    QuinielaTheme {
        AdminPartidoCard(
            partido = partidoFake("p3", local = "España", visitante = "Alemania", estado = EstadoPartido.EN_VIVO, excluido = true),
            onActualizar = { _, _, _ -> },
            onCambiarEstado = { _, _ -> },
            onToggleExcluido = {},
            onAbrirPredicciones = { _, _ -> },
            onForzarBloqueo = {},
        )
    }
}

@Preview(showBackground = true, name = "Admin — tarjeta con ventana abierta")
@Composable
private fun AdminPartidoCardVentanaAbertaPreview() {
    QuinielaTheme {
        AdminPartidoCard(
            partido = partidoFake(
                "p4", local = "México", visitante = "EUA",
                estado = EstadoPartido.PROGRAMADO,
            ).copy(
                lockOverride = false,
                lockOverrideExpiry = java.time.Instant.now().plusSeconds(1800),
            ),
            onActualizar = { _, _, _ -> },
            onCambiarEstado = { _, _ -> },
            onToggleExcluido = {},
            onAbrirPredicciones = { _, _ -> },
            onForzarBloqueo = {},
        )
    }
}
