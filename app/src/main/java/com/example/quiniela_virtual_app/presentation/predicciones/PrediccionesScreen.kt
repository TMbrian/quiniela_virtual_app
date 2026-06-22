package com.example.quiniela_virtual_app.presentation.predicciones

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.presentation.partidos.PartidoConPrediccion
import com.example.quiniela_virtual_app.presentation.partidos.PartidoGrupoSeccion
import com.example.quiniela_virtual_app.presentation.partidos.PartidoJornadaSeccion
import com.example.quiniela_virtual_app.presentation.partidos.PartidosViewModel
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.enEspañol
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrediccionesScreen(
    onVerPartido: () -> Unit = {},
    viewModel: PartidosViewModel = hiltViewModel(),
) {
    val secciones by viewModel.secciones.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val guardarError by viewModel.guardarError.collectAsState()
    val mensajeExito by viewModel.mensajeExito.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val proximoPartido by viewModel.proximoPartido.collectAsState()
    val filtroJornada by viewModel.filtroJornada.collectAsState()
    val jornadasDisponibles by viewModel.jornadasDisponibles.collectAsState()
    val lockMs = (uiState as? UiState.Success)?.data?.lockAnticipacionMs ?: 60 * 60_000L
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(guardarError) {
        guardarError?.let { snackbarHost.showSnackbar(it); viewModel.limpiarError() }
    }
    LaunchedEffect(mensajeExito) {
        mensajeExito?.let { snackbarHost.showSnackbar(it); viewModel.limpiarMensajeExito() }
    }

    Column(Modifier.fillMaxSize()) {
        if (jornadasDisponibles.size > 1) {
            JornadaFiltroChips(
                filtroJornada = filtroJornada,
                onJornadaChange = viewModel::filtrarJornada,
                jornadasDisponibles = jornadasDisponibles,
            )
            HorizontalDivider()
        }
        Box(Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::reiniciar,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val estado = secciones) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error   -> ErrorMessage(estado.mensaje)
                    is UiState.Success -> ContenidoPredicciones(
                        secciones = filtrarSoloAbiertas(estado.data),
                        lockAnticipacionMs = lockMs,
                        proximoPartido = proximoPartido,
                        onVerPartido = onVerPartido,
                        onGuardar = viewModel::guardarPrediccion,
                    )
                }
            }
            SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun filtrarSoloAbiertas(secciones: List<PartidoJornadaSeccion>): List<PartidoJornadaSeccion> =
    secciones.mapNotNull { seccion ->
        val gruposFiltrados = seccion.grupos.mapNotNull { grupo ->
            val abiertos = grupo.items.filter { it.estadoPrediccion == EstadoPrediccion.ABIERTO }
            if (abiertos.isEmpty()) null else PartidoGrupoSeccion(grupo.nombre, abiertos)
        }
        if (gruposFiltrados.isEmpty()) null
        else PartidoJornadaSeccion(seccion.titulo, seccion.subtitulo, gruposFiltrados)
    }

@Composable
private fun ContenidoPredicciones(
    secciones: List<PartidoJornadaSeccion>,
    lockAnticipacionMs: Long,
    proximoPartido: PartidoConPrediccion?,
    onVerPartido: () -> Unit,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    if (secciones.isEmpty() && proximoPartido == null) {
        EstadoVacio()
        return
    }

    val todosLosItems = secciones.flatMap { s -> s.grupos.flatMap { it.items } }
    val totalAbiertas = todosLosItems.size
    val predichas = todosLosItems.count { it.prediccion != null }

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "resumen") {
            ResumenCard(predichas = predichas, total = totalAbiertas)
        }
        if (proximoPartido != null) {
            item(key = "proximo") {
                ProximoPartidoCard(
                    item = proximoPartido,
                    lockAnticipacionMs = lockAnticipacionMs,
                    onVerPartido = onVerPartido,
                )
            }
        }
        secciones.forEach { seccion ->
            val itemsSeccion = seccion.grupos.flatMap { it.items }
            val predichasEnSeccion = itemsSeccion.count { it.prediccion != null }
            stickyHeader(key = "hdr_${seccion.titulo}") {
                JornadaPrediccionesHeader(
                    titulo = seccion.titulo,
                    predichas = predichasEnSeccion,
                    total = itemsSeccion.size,
                )
            }
            items(itemsSeccion, key = { it.partido.id }) { item ->
                TarjetaPrediccion(
                    item = item,
                    lockAnticipacionMs = lockAnticipacionMs,
                    onGuardar = onGuardar,
                )
            }
        }
    }
}

@Composable
private fun ProximoPartidoCard(
    item: PartidoConPrediccion,
    lockAnticipacionMs: Long,
    onVerPartido: () -> Unit,
) {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            ahora = System.currentTimeMillis()
        }
    }
    val restanteMs = item.partido.fecha.toEpochMilli() - lockAnticipacionMs - ahora
    val mostrarCierre = item.estadoPrediccion == EstadoPrediccion.ABIERTO && restanteMs in 1..<86_400_000L

    Card(
        onClick = onVerPartido,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Próximo partido",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                when {
                    mostrarCierre           -> CuentaRegresiva(restanteMs)
                    item.prediccion != null -> ConfirmacionChip(
                        goles = "${item.prediccion.golesLocal} - ${item.prediccion.golesVisitante}",
                    )
                    else -> Text(
                        text = "Aún sin predecir",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FilaEquipos(item.partido)
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.partido.fecha.toFechaCorta(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ver en Partidos →",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun ResumenCard(predichas: Int, total: Int) {
    val progreso = if (total > 0) predichas / total.toFloat() else 0f
    val colorProgreso = when {
        progreso >= 1f -> MaterialTheme.colorScheme.primary
        progreso > 0f  -> MaterialTheme.colorScheme.secondary
        else           -> MaterialTheme.colorScheme.outline
    }
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
                    text = "Mis predicciones",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$predichas / $total",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorProgreso,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth(),
                color = colorProgreso,
            )
            if (predichas < total) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Te faltan ${total - predichas} predicciones por completar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "¡Todas tus predicciones están listas!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun JornadaPrediccionesHeader(titulo: String, predichas: Int, total: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
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
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "$predichas/$total predicciones",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun TarjetaPrediccion(
    item: PartidoConPrediccion,
    lockAnticipacionMs: Long,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    val borde = if (item.prediccion != null)
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    else null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = borde,
    ) {
        Column(Modifier.padding(12.dp)) {
            FilaEquipos(item.partido)
            Spacer(Modifier.height(4.dp))
            FechaPartido(item.partido)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            FormularioPrediccion(
                item = item,
                lockAnticipacionMs = lockAnticipacionMs,
                onGuardar = onGuardar,
            )
        }
    }
}

@Composable
private fun FilaEquipos(partido: Partido) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        EquipoColumnPrediccion(
            nombre = partido.equipoLocal.nombre.enEspañol(),
            crestUrl = partido.equipoLocal.crestUrl,
            iso = partido.equipoLocal.iso,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "vs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        EquipoColumnPrediccion(
            nombre = partido.equipoVisitante.nombre.enEspañol(),
            crestUrl = partido.equipoVisitante.crestUrl,
            iso = partido.equipoVisitante.iso,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EquipoColumnPrediccion(
    nombre: String,
    crestUrl: String?,
    iso: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SubcomposeAsyncImage(
            model = crestUrl,
            contentDescription = nombre,
            modifier = Modifier.size(44.dp),
            contentScale = ContentScale.Fit,
            loading = {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            },
            error = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
private fun FechaPartido(partido: Partido) {
    val texto = partido.fecha.toFechaCorta()
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FormularioPrediccion(
    item: PartidoConPrediccion,
    lockAnticipacionMs: Long,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    val context = LocalContext.current
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
            if (restanteMs in 1..<86_400_000L) CuentaRegresiva(restanteMs)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CampoPuntaje(valor = gL, label = "L", onCambio = { if (it.length <= 2) gL = it })
            Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CampoPuntaje(valor = gV, label = "V", onCambio = { if (it.length <= 2) gV = it })
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
        if (item.prediccion != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ConfirmacionChip(goles = "${item.prediccion.golesLocal} - ${item.prediccion.golesVisitante}")
                IconButton(
                    onClick = {
                        compartirPrediccion(
                            context,
                            item.partido,
                            item.prediccion.golesLocal,
                            item.prediccion.golesVisitante,
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir predicción",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmacionChip(goles: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = "Guardada: $goles",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun CuentaRegresiva(restanteMs: Long) {
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
private fun CampoPuntaje(valor: String, label: String, onCambio: (String) -> Unit) {
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

@Composable
private fun EstadoVacio() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "¡Todo listo!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "No hay partidos abiertos para predecir en este momento.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun compartirPrediccion(
    context: Context,
    partido: Partido,
    golesLocal: Int,
    golesVisitante: Int,
) {
    val local = partido.equipoLocal.nombre.enEspañol()
    val visitante = partido.equipoVisitante.nombre.enEspañol()
    val fecha = partido.fecha.toFechaCorta()
    val texto = "Mi predicción: $local $golesLocal - $golesVisitante $visitante\n$fecha\n— Quiniela Virtual"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private val fechaFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-MX"))

private fun Instant.toFechaCorta(): String =
    atZone(ZoneId.systemDefault()).format(fechaFormatter)
