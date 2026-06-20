package com.example.quiniela_virtual_app.presentation.partidos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import java.time.Instant
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.EstadoBadge
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator

@Composable
fun PartidosScreen(viewModel: PartidosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val guardarError by viewModel.guardarError.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(guardarError) {
        guardarError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (val estado = uiState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error   -> ErrorMessage(estado.mensaje)
            is UiState.Success -> PartidosList(estado.data, viewModel::guardarPrediccion)
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PartidosList(
    data: PartidosData,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    if (data.partidos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("El administrador aún no ha cargado partidos.")
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
        items(data.partidos, key = { it.partido.id }) { item ->
            PartidoCard(item = item, onGuardar = onGuardar)
        }
    }
}

@Composable
private fun PartidoCard(
    item: PartidoConPrediccion,
    onGuardar: (Partido, Int, Int) -> Unit,
) {
    val bloqueado = item.estadoPrediccion == EstadoPrediccion.BLOQUEADO
    var golesLocal by remember(item.prediccion) {
        mutableStateOf(item.prediccion?.golesLocal?.toString() ?: "")
    }
    var golesVisitante by remember(item.prediccion) {
        mutableStateOf(item.prediccion?.golesVisitante?.toString() ?: "")
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = cardColors(bloqueado),
    ) {
        Column(Modifier.padding(16.dp)) {
            CabeceraPartido(item)
            Spacer(Modifier.height(12.dp))
            FilaPrediccion(
                golesLocal = golesLocal,
                golesVisitante = golesVisitante,
                bloqueado = bloqueado,
                onGolesLocalChange = { golesLocal = it },
                onGolesVisitanteChange = { golesVisitante = it },
            )
            if (!bloqueado) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val gL = golesLocal.toIntOrNull() ?: return@Button
                        val gV = golesVisitante.toIntOrNull() ?: return@Button
                        onGuardar(item.partido, gL, gV)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (item.prediccion != null) "Actualizar" else "Guardar")
                }
            }
        }
    }
}

@Composable
private fun CabeceraPartido(item: PartidoConPrediccion) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.partido.equipoLocal.nombre, style = MaterialTheme.typography.bodyLarge)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("vs", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            EstadoBadge(item.partido.estado)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.partido.equipoVisitante.nombre, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun FilaPrediccion(
    golesLocal: String,
    golesVisitante: String,
    bloqueado: Boolean,
    onGolesLocalChange: (String) -> Unit,
    onGolesVisitanteChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = golesLocal,
            onValueChange = { if (it.length <= 2) onGolesLocalChange(it) },
            modifier = Modifier.width(72.dp),
            singleLine = true,
            enabled = !bloqueado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Local") },
        )
        Text(" - ", modifier = Modifier.padding(horizontal = 8.dp))
        OutlinedTextField(
            value = golesVisitante,
            onValueChange = { if (it.length <= 2) onGolesVisitanteChange(it) },
            modifier = Modifier.width(72.dp),
            singleLine = true,
            enabled = !bloqueado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Visita") },
        )
    }
    if (bloqueado) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Partido bloqueado para predicciones",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun cardColors(bloqueado: Boolean) = if (bloqueado) {
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
} else {
    CardDefaults.cardColors()
}

private fun partidoFake() = Partido(
    id = "p1", jornada = 1, grupo = "A", fase = "Grupos",
    equipoLocal = Equipo("México", "MX"),
    equipoVisitante = Equipo("Estados Unidos", "US"),
    fecha = Instant.now(), sede = "Estadio Azteca",
    golesLocal = null, golesVisitante = null,
    estado = EstadoPartido.PROGRAMADO,
)

@Preview(showBackground = true, name = "Partido — abierto sin predicción")
@Composable
private fun PartidoCardAbiertoPreview() {
    QuinielaTheme {
        PartidoCard(
            item = PartidoConPrediccion(partidoFake(), null, EstadoPrediccion.ABIERTO),
            onGuardar = { _, _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Partido — bloqueado con predicción")
@Composable
private fun PartidoCardBloqueadoPreview() {
    val pred = Prediccion("u1", "p1", 2, 1, Instant.now(), Instant.now())
    QuinielaTheme {
        PartidoCard(
            item = PartidoConPrediccion(partidoFake(), pred, EstadoPrediccion.BLOQUEADO),
            onGuardar = { _, _, _ -> },
        )
    }
}
