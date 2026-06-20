package com.example.quiniela_virtual_app.presentation.admin.config

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import java.time.Instant
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

@Composable
fun AdminConfigScreen(viewModel: AdminConfigViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val mensaje by viewModel.guardadoEstado.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let { snackbarHost.showSnackbar(it); viewModel.limpiarMensaje() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { paddingValues ->
        when (val estado = uiState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error   -> ErrorMessage(estado.mensaje)
            is UiState.Success -> ConfigForm(
                config = estado.data,
                onGuardar = { n, d, t, pE, pT, lM, a -> viewModel.guardar(n, d, t, pE, pT, lM, a) },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun ConfigForm(
    config: ConfiguracionApp,
    onGuardar: (String, String, String, Int, Int, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nombre by remember(config) { mutableStateOf(config.nombreCompeticion) }
    var descripcion by remember(config) { mutableStateOf(config.descripcion) }
    var temporada by remember(config) { mutableStateOf(config.temporada) }
    var puntosExacto by remember(config) { mutableStateOf(config.puntosExacto.toString()) }
    var puntosTendencia by remember(config) { mutableStateOf(config.puntosTendencia.toString()) }
    var lockMin by remember(config) { mutableStateOf(config.lockAnticipacionMin.toString()) }
    var activa by remember(config) { mutableStateOf(config.activa) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Configuración de la quiniela", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        CampoTexto("Nombre de la competición", nombre) { nombre = it }
        CampoTexto("Descripción", descripcion) { descripcion = it }
        CampoTexto("Temporada", temporada) { temporada = it }

        Spacer(Modifier.height(8.dp))
        CampoNumero("Puntos por acierto exacto", puntosExacto) { puntosExacto = it }
        CampoNumero("Puntos por tendencia correcta", puntosTendencia) { puntosTendencia = it }
        CampoNumero("Minutos de anticipación para bloqueo", lockMin) { lockMin = it }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quiniela activa", modifier = Modifier.weight(1f))
            Switch(checked = activa, onCheckedChange = { activa = it })
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onGuardar(
                    nombre, descripcion, temporada,
                    puntosExacto.toIntOrNull() ?: config.puntosExacto,
                    puntosTendencia.toIntOrNull() ?: config.puntosTendencia,
                    lockMin.toIntOrNull() ?: config.lockAnticipacionMin,
                    activa,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Guardar configuración") }
    }
}

@Composable
private fun CampoTexto(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
    )
}

@Composable
private fun CampoNumero(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 4) onValueChange(it) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Preview(showBackground = true, name = "Admin — configuración")
@Composable
private fun ConfigFormPreview() {
    QuinielaTheme {
        ConfigForm(
            config = ConfiguracionApp(
                nombreCompeticion = "Quiniela Mundial 2026",
                descripcion = "Quiniela privada de amigos",
                temporada = "2026",
                puntosExacto = 3,
                puntosTendencia = 1,
                lockAnticipacionMin = 60,
                activa = true,
                creadaEn = Instant.now(),
                actualizadaEn = Instant.now(),
            ),
            onGuardar = { _, _, _, _, _, _, _ -> },
        )
    }
}
