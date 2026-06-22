package com.example.quiniela_virtual_app.presentation.partidos

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
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
fun PartidosScreen(viewModel: PartidosViewModel = hiltViewModel()) {
    val secciones by viewModel.secciones.collectAsState()
    val uiStateValue by viewModel.uiState.collectAsState()
    val filtro by viewModel.filtroEstado.collectAsState()
    val guardarError by viewModel.guardarError.collectAsState()
    val mensajeExito by viewModel.mensajeExito.collectAsState()
    val lockMs = (uiStateValue as? UiState.Success)?.data?.lockAnticipacionMs ?: 60 * 60_000L
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(guardarError) {
        guardarError?.let { snackbarHost.showSnackbar(it); viewModel.limpiarError() }
    }
    LaunchedEffect(mensajeExito) {
        mensajeExito?.let { snackbarHost.showSnackbar(it); viewModel.limpiarMensajeExito() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            FiltrosChips(filtroActual = filtro, onFiltroChange = viewModel::filtrarPor)
            HorizontalDivider()
            when (val estado = secciones) {
                is UiState.Loading -> LoadingIndicator()
                is UiState.Error   -> ErrorMessage(estado.mensaje)
                is UiState.Success -> PartidosPorSecciones(
                    secciones = estado.data,
                    lockAnticipacionMs = lockMs,
                    onGuardar = viewModel::guardarPrediccion,
                )
            }
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private val FILTROS: List<Pair<EstadoPartido?, String>> = listOf(
    null to "Todos",
    EstadoPartido.EN_VIVO to "En vivo",
    EstadoPartido.PROGRAMADO to "Por jugar",
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
    secciones: List<PartidoJornadaSeccion>,
    lockAnticipacionMs: Long,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    if (secciones.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("El administrador aún no ha cargado partidos.")
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
                items(grupo.items, key = { it.partido.id }) { item ->
                    PartidoCard(item = item, lockAnticipacionMs = lockAnticipacionMs, onGuardar = onGuardar)
                }
            }
        }
    }
}

private data class ResumenJornada(
    val conPrediccion: Int,
    val total: Int,
    val puntosGanados: Int,
    val hayFinalizados: Boolean,
)

private fun resumenDe(seccion: PartidoJornadaSeccion): ResumenJornada {
    val items = seccion.grupos.flatMap { it.items }
    return ResumenJornada(
        conPrediccion = items.count { it.prediccion != null },
        total = items.size,
        puntosGanados = items.mapNotNull { it.puntosGanados }.sum(),
        hayFinalizados = items.any { it.partido.estado == EstadoPartido.FINALIZADO && it.prediccion != null },
    )
}

@Composable
private fun JornadaHeader(seccion: PartidoJornadaSeccion) {
    val resumen = resumenDe(seccion)
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            ResumenJornadaRow(resumen)
        }
    }
}

@Composable
private fun ResumenJornadaRow(resumen: ResumenJornada) {
    val colorMuted = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
    val colorPts = if (resumen.puntosGanados > 0) MaterialTheme.colorScheme.primary else colorMuted
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${resumen.conPrediccion}/${resumen.total} predicciones",
            style = MaterialTheme.typography.labelSmall,
            color = colorMuted,
        )
        if (resumen.hayFinalizados) {
            Text("·", style = MaterialTheme.typography.labelSmall, color = colorMuted)
            Text(
                text = "+${resumen.puntosGanados} pts",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorPts,
            )
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
private fun PartidoCard(
    item: PartidoConPrediccion,
    lockAnticipacionMs: Long,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    val bloqueado = item.estadoPrediccion == EstadoPrediccion.BLOQUEADO
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        colors = if (bloqueado) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                 else CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            CabeceraPartidoCard(item.partido)
            Spacer(Modifier.height(10.dp))
            FilaEquiposYMarcador(item.partido)
            Spacer(Modifier.height(6.dp))
            FechaYSede(item.partido)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            SeccionPrediccion(item = item, lockAnticipacionMs = lockAnticipacionMs, onGuardar = onGuardar)
        }
    }
}

@Composable
private fun CabeceraPartidoCard(partido: Partido) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EstadoBadge(partido.estado)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = if (partido.fase == "Grupos") "J${partido.jornada}" else partido.fase,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
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
    val inicialesColors = MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SubcomposeAsyncImage(
            model = equipo.crestUrl,
            contentDescription = equipo.nombre,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit,
            loading = {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            },
            error = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color = inicialesColors.first, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = equipo.iso.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = inicialesColors.second,
                    )
                }
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = equipo.nombre.enEspañol(),
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
private fun SeccionPrediccion(
    item: PartidoConPrediccion,
    lockAnticipacionMs: Long,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    when {
        item.estadoPrediccion == EstadoPrediccion.ABIERTO ->
            FormularioPrediccion(item = item, lockAnticipacionMs = lockAnticipacionMs, onGuardar = onGuardar)
        item.partido.estado == EstadoPartido.FINALIZADO ->
            ResultadoFinalizadoSection(item)
        else ->
            PrediccionBloqueadaSection(prediccion = item.prediccion)
    }
}

@Composable
private fun FormularioPrediccion(
    item: PartidoConPrediccion,
    lockAnticipacionMs: Long,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            ahora = System.currentTimeMillis()
        }
    }
    val restanteMs = item.partido.fecha.toEpochMilli() - lockAnticipacionMs - ahora

    var gL by remember(item.prediccion) {
        mutableStateOf(item.prediccion?.golesLocal?.toString() ?: "")
    }
    var gV by remember(item.prediccion) {
        mutableStateOf(item.prediccion?.golesVisitante?.toString() ?: "")
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (item.prediccion != null) "Tu predicción" else "Predice el marcador",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
            if (restanteMs in 1..<86_400_000L) CountdownChip(restanteMs)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedFieldGoles(valor = gL, label = "L", onCambio = { if (it.length <= 2) gL = it })
            Text(text = "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedFieldGoles(valor = gV, label = "V", onCambio = { if (it.length <= 2) gV = it })
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val golesL = gL.toIntOrNull() ?: return@Button
                    val golesV = gV.toIntOrNull() ?: return@Button
                    onGuardar(item.partido, golesL, golesV)
                },
            ) {
                Text(if (item.prediccion != null) "Actualizar" else "Guardar")
            }
        }
    }
}

@Composable
private fun CountdownChip(restanteMs: Long) {
    val h = restanteMs / 3_600_000
    val m = (restanteMs % 3_600_000) / 60_000
    val s = (restanteMs % 60_000) / 1_000
    val texto = when {
        h > 0 -> "${h}h ${m.toString().padStart(2, '0')}m"
        m > 0 -> "${m}m ${s.toString().padStart(2, '0')}s"
        else  -> "${s}s"
    }
    Text(
        text = "Cierra en $texto",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun OutlinedFieldGoles(valor: String, label: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = Modifier.width(64.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text(label) },
    )
}

@Composable
private fun ResultadoFinalizadoSection(item: PartidoConPrediccion) {
    if (item.prediccion == null) {
        Text(
            text = "Sin predicción registrada",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Tu pred: ${item.prediccion.golesLocal} - ${item.prediccion.golesVisitante}",
                style = MaterialTheme.typography.bodySmall,
            )
            val badge = calcularResultadoBadge(item.partido, item.prediccion)
            if (badge != null) ResultadoBadge(badge)
        }
        if (item.puntosGanados != null) PuntosChip(item.puntosGanados)
    }
}

@Composable
private fun PuntosChip(puntos: Int) {
    val colorPositivo = MaterialTheme.colorScheme.primary
    val colorNeutro = MaterialTheme.colorScheme.outline
    val (texto, color) = if (puntos > 0) "+$puntos pts" to colorPositivo else "0 pts" to colorNeutro
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
private fun ResultadoBadge(badge: ResultadoBadgeInfo) {
    Text(
        text = badge.etiqueta,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .background(color = badge.color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun PrediccionBloqueadaSection(prediccion: Prediccion?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Bloqueado",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        if (prediccion != null) {
            Text(
                text = "· Tu predicción: ${prediccion.golesLocal} - ${prediccion.golesVisitante}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private data class ResultadoBadgeInfo(val etiqueta: String, val color: Color)

private fun calcularResultadoBadge(partido: Partido, pred: Prediccion): ResultadoBadgeInfo? {
    if (partido.estado != EstadoPartido.FINALIZADO) return null
    val gL = partido.golesLocal ?: return null
    val gV = partido.golesVisitante ?: return null
    return when {
        gL == pred.golesLocal && gV == pred.golesVisitante ->
            ResultadoBadgeInfo("Exacto", Color(0xFF4CAF50))
        mismoGanador(gL, gV, pred.golesLocal, pred.golesVisitante) ->
            ResultadoBadgeInfo("Tendencia", Color(0xFFFFA726))
        else ->
            ResultadoBadgeInfo("Fallo", Color(0xFFEF5350))
    }
}

private fun mismoGanador(gL: Int, gV: Int, pL: Int, pV: Int): Boolean =
    (gL > gV && pL > pV) || (gL < gV && pL < pV) || (gL == gV && pL == pV)

private val fechaFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-MX"))

private fun Instant.toFechaCorta(): String =
    atZone(ZoneId.systemDefault()).format(fechaFormatter)

private val NOMBRES_ES: Map<String, String> = mapOf(
    // Asia
    "Korea Republic"    to "Corea del Sur",
    "Korea DPR"         to "Corea del Norte",
    "IR Iran"           to "Irán",
    "Saudi Arabia"      to "Arabia Saudita",
    "Japan"             to "Japón",
    "China PR"          to "China",
    "Uzbekistan"        to "Uzbekistán",
    "Indonesia"         to "Indonesia",
    "Jordan"            to "Jordania",
    "Iraq"              to "Irak",
    "Bahrain"           to "Baréin",
    "Oman"              to "Omán",
    // África
    "South Africa"      to "Sudáfrica",
    "Morocco"           to "Marruecos",
    "Cameroon"          to "Camerún",
    "Egypt"             to "Egipto",
    "Côte d'Ivoire"     to "Costa de Marfil",
    "DR Congo"          to "Congo RD",
    "Algeria"           to "Argelia",
    "Tunisia"           to "Túnez",
    "Nigeria"           to "Nigeria",
    "Senegal"           to "Senegal",
    "Ghana"             to "Ghana",
    "Mali"              to "Mali",
    // Europa
    "Germany"           to "Alemania",
    "France"            to "Francia",
    "Netherlands"       to "Países Bajos",
    "Belgium"           to "Bélgica",
    "Croatia"           to "Croacia",
    "England"           to "Inglaterra",
    "Serbia"            to "Serbia",
    "Switzerland"       to "Suiza",
    "Denmark"           to "Dinamarca",
    "Austria"           to "Austria",
    "Scotland"          to "Escocia",
    "Romania"           to "Rumanía",
    "Slovakia"          to "Eslovaquia",
    "Hungary"           to "Hungría",
    "Czechia"           to "República Checa",
    "Poland"            to "Polonia",
    "Slovenia"          to "Eslovenia",
    "Türkiye"           to "Turquía",
    "Ukraine"           to "Ucrania",
    "Greece"            to "Grecia",
    "Wales"             to "Gales",
    "Sweden"            to "Suecia",
    "Norway"            to "Noruega",
    "Finland"           to "Finlandia",
    // América del Sur
    "Brazil"            to "Brasil",
    "Peru"              to "Perú",
    "Paraguay"          to "Paraguay",
    "Venezuela"         to "Venezuela",
    "Ecuador"           to "Ecuador",
    "Bolivia"           to "Bolivia",
    // América del Norte/Centro/Caribe
    "United States"     to "Estados Unidos",
    "Costa Rica"        to "Costa Rica",
    "Panama"            to "Panamá",
    "Honduras"          to "Honduras",
    "Jamaica"           to "Jamaica",
    "El Salvador"       to "El Salvador",
    "Cuba"              to "Cuba",
    "Trinidad and Tobago" to "Trinidad y Tobago",
    // Oceanía
    "New Zealand"       to "Nueva Zelanda",
)

private fun String.enEspañol(): String = NOMBRES_ES[this] ?: this

// ── Datos para previews ──────────────────────────────────────────────────────

private fun partidoFake(
    id: String = "p1",
    golesLocal: Int? = null,
    golesVisitante: Int? = null,
    estado: EstadoPartido = EstadoPartido.PROGRAMADO,
) = Partido(
    id = id, jornada = 1, grupo = "A", fase = "Grupos",
    equipoLocal = Equipo("México", "MX"),
    equipoVisitante = Equipo("Argentina", "AR"),
    fecha = Instant.now(), sede = "Estadio Azteca",
    golesLocal = golesLocal, golesVisitante = golesVisitante,
    estado = estado,
)

private fun prediccionFake() = Prediccion("u1", "p1", 2, 1, Instant.now(), Instant.now())

@Preview(showBackground = true, name = "Partido — abierto sin predicción")
@Composable
private fun PartidoCardAbiertoPreview() {
    QuinielaTheme {
        PartidoCard(
            item = PartidoConPrediccion(partidoFake(), null, EstadoPrediccion.ABIERTO, null),
            lockAnticipacionMs = 30 * 60_000L,
            onGuardar = { _, _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Partido — abierto con predicción")
@Composable
private fun PartidoCardConPrediccionPreview() {
    QuinielaTheme {
        PartidoCard(
            item = PartidoConPrediccion(partidoFake(), prediccionFake(), EstadoPrediccion.ABIERTO, null),
            lockAnticipacionMs = 30 * 60_000L,
            onGuardar = { _, _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Partido — finalizado exacto (+5 pts)")
@Composable
private fun PartidoCardFinalizadoPreview() {
    QuinielaTheme {
        PartidoCard(
            item = PartidoConPrediccion(
                partido = partidoFake(golesLocal = 2, golesVisitante = 1, estado = EstadoPartido.FINALIZADO),
                prediccion = prediccionFake(),
                estadoPrediccion = EstadoPrediccion.BLOQUEADO,
                puntosGanados = 5,
            ),
            lockAnticipacionMs = 30 * 60_000L,
            onGuardar = { _, _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Partido — bloqueado sin predicción")
@Composable
private fun PartidoCardBloqueadoPreview() {
    QuinielaTheme {
        PartidoCard(
            item = PartidoConPrediccion(partidoFake(estado = EstadoPartido.EN_VIVO), null, EstadoPrediccion.BLOQUEADO, null),
            lockAnticipacionMs = 30 * 60_000L,
            onGuardar = { _, _, _ -> },
        )
    }
}
