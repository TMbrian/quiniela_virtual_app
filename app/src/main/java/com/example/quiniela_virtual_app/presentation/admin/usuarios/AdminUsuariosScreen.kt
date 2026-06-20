package com.example.quiniela_virtual_app.presentation.admin.usuarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import java.time.Instant
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator

@Composable
fun AdminUsuariosScreen(viewModel: AdminUsuariosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val mensaje by viewModel.accionEstado.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let { snackbarHost.showSnackbar(it); viewModel.limpiarMensaje() }
    }

    Box(Modifier.fillMaxSize()) {
        when (val estado = uiState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error   -> ErrorMessage(estado.mensaje)
            is UiState.Success -> UsuariosList(
                usuarios = estado.data,
                onToggleAdmin = viewModel::toggleAdmin,
                onToggleActivo = viewModel::toggleActivo,
            )
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun UsuariosList(
    usuarios: List<Usuario>,
    onToggleAdmin: (Usuario) -> Unit,
    onToggleActivo: (Usuario) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(usuarios, key = { it.uid }) { usuario ->
            UsuarioCard(usuario = usuario, onToggleAdmin = onToggleAdmin, onToggleActivo = onToggleActivo)
        }
    }
}

@Composable
private fun UsuarioCard(
    usuario: Usuario,
    onToggleAdmin: (Usuario) -> Unit,
    onToggleActivo: (Usuario) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(usuario.nombre, style = MaterialTheme.typography.bodyLarge)
            Text(usuario.email, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onToggleAdmin(usuario) }) {
                    Text(if (usuario.esAdmin) "Quitar admin" else "Dar admin")
                }
                OutlinedButton(onClick = { onToggleActivo(usuario) }) {
                    Text(if (usuario.activo) "Deshabilitar" else "Habilitar")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Admin — usuario normal")
@Composable
private fun UsuarioCardPreview() {
    QuinielaTheme {
        UsuarioCard(
            usuario = Usuario(
                uid = "u1", nombre = "Brian Tzuc", email = "brian@ejemplo.com",
                fotoUrl = null, esAdmin = false, activo = true,
                creadoEn = Instant.now(), ultimoAcceso = Instant.now(),
            ),
            onToggleAdmin = {},
            onToggleActivo = {},
        )
    }
}

@Preview(showBackground = true, name = "Admin — usuario deshabilitado")
@Composable
private fun UsuarioCardDeshabilitadoPreview() {
    QuinielaTheme {
        UsuarioCard(
            usuario = Usuario(
                uid = "u2", nombre = "Ana García", email = "ana@ejemplo.com",
                fotoUrl = null, esAdmin = false, activo = false,
                creadoEn = Instant.now(), ultimoAcceso = Instant.now(),
            ),
            onToggleAdmin = {},
            onToggleActivo = {},
        )
    }
}
