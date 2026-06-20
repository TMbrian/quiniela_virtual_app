package com.example.quiniela_virtual_app.presentation.admin.partidos

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.EstadoBadge
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator

@Composable
fun AdminPartidosScreen(viewModel: AdminPartidosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val cargando by viewModel.cargandoAccion.collectAsState()
    val mensaje by viewModel.accionEstado.collectAsState()
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
            when (val estado = uiState) {
                is UiState.Loading -> LoadingIndicator()
                is UiState.Error   -> ErrorMessage(estado.mensaje)
                is UiState.Success -> PartidosAdminList(
                    partidos = estado.data,
                    onActualizar = viewModel::actualizarResultado,
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

@Composable
private fun PartidosAdminList(
    partidos: List<Partido>,
    onActualizar: (Partido, Int, Int) -> Unit,
) {
    if (partidos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay partidos. Sincroniza desde la API.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(partidos, key = { it.id }) { partido ->
            AdminPartidoCard(partido = partido, onActualizar = onActualizar)
        }
    }
}

@Composable
private fun AdminPartidoCard(
    partido: Partido,
    onActualizar: (Partido, Int, Int) -> Unit,
) {
    var golesLocal by remember { mutableStateOf(partido.golesLocal?.toString() ?: "") }
    var golesVisitante by remember { mutableStateOf(partido.golesVisitante?.toString() ?: "") }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${partido.equipoLocal.nombre} vs ${partido.equipoVisitante.nombre}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                EstadoBadge(partido.estado)
            }
            Spacer(Modifier.height(8.dp))
            Text("Jornada ${partido.jornada}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = golesLocal,
                    onValueChange = { if (it.length <= 2) golesLocal = it },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Local") },
                )
                Text("-")
                OutlinedTextField(
                    value = golesVisitante,
                    onValueChange = { if (it.length <= 2) golesVisitante = it },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Visita") },
                )
                Button(
                    onClick = {
                        val gL = golesLocal.toIntOrNull() ?: return@Button
                        val gV = golesVisitante.toIntOrNull() ?: return@Button
                        onActualizar(partido, gL, gV)
                    },
                ) { Text("OK") }
            }
        }
    }
}
