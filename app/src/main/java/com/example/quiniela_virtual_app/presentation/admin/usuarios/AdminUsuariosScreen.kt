package com.example.quiniela_virtual_app.presentation.admin.usuarios

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            is UiState.Success -> ListaUsuarios(
                usuarios      = estado.data,
                onToggleAdmin = viewModel::toggleAdmin,
                onToggleActivo = viewModel::toggleActivo,
            )
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ListaUsuarios(
    usuarios: List<Usuario>,
    onToggleAdmin: (Usuario) -> Unit,
    onToggleActivo: (Usuario) -> Unit,
) {
    if (usuarios.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay usuarios registrados.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }
    val ordenados = usuarios.sortedWith(
        compareByDescending<Usuario> { it.esAdmin }
            .thenByDescending { it.activo }
            .thenBy { it.nombre },
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp, vertical = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "conteo") {
            EncabezadoConteo(total = ordenados.size)
        }
        items(ordenados, key = { it.uid }) { usuario ->
            UsuarioCard(
                usuario       = usuario,
                onToggleAdmin = onToggleAdmin,
                onToggleActivo = onToggleActivo,
            )
        }
    }
}

@Composable
private fun EncabezadoConteo(total: Int) {
    Text(
        text = "$total participante${if (total == 1) "" else "s"}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun UsuarioCard(
    usuario: Usuario,
    onToggleAdmin: (Usuario) -> Unit,
    onToggleActivo: (Usuario) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (usuario.activo) 1f else 0.55f),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            FilaPrincipal(usuario)
            Spacer(Modifier.height(4.dp))
            UltimoAcceso(usuario.ultimoAcceso)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            BotonesAccion(
                usuario        = usuario,
                onToggleAdmin  = onToggleAdmin,
                onToggleActivo = onToggleActivo,
            )
        }
    }
}

@Composable
private fun FilaPrincipal(usuario: Usuario) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AvatarInicial(nombre = usuario.nombre)
        Column(Modifier.weight(1f)) {
            Text(
                text = usuario.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = usuario.email,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        BadgesEstado(usuario)
    }
}

@Composable
private fun AvatarInicial(nombre: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nombre.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun BadgesEstado(usuario: Usuario) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (usuario.esAdmin) {
            InsigniaBadge(
                texto      = "Admin",
                colorFondo = MaterialTheme.colorScheme.primaryContainer,
                colorTexto = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        val (colorActivoBg, colorActivoFg) = if (usuario.activo)
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer

        InsigniaBadge(
            texto      = if (usuario.activo) "Activo" else "Deshabilitado",
            colorFondo = colorActivoBg,
            colorTexto = colorActivoFg,
        )
    }
}

@Composable
private fun InsigniaBadge(texto: String, colorFondo: Color, colorTexto: Color) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = colorTexto,
        modifier = Modifier
            .background(color = colorFondo, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun UltimoAcceso(ultimoAcceso: Instant) {
    Text(
        text = "Último acceso: ${ultimoAcceso.toAccesoTexto()}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun BotonesAccion(
    usuario: Usuario,
    onToggleAdmin: (Usuario) -> Unit,
    onToggleActivo: (Usuario) -> Unit,
) {
    val coloresDeshabilitar = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.error,
    )
    val coloresNormal = ButtonDefaults.outlinedButtonColors()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { onToggleAdmin(usuario) },
            modifier = Modifier.weight(1f),
        ) {
            Text(if (usuario.esAdmin) "Quitar admin" else "Dar admin")
        }
        OutlinedButton(
            onClick = { onToggleActivo(usuario) },
            modifier = Modifier.weight(1f),
            colors = if (usuario.activo) coloresDeshabilitar else coloresNormal,
        ) {
            Text(if (usuario.activo) "Deshabilitar" else "Habilitar")
        }
    }
}

private val accesoFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-MX"))

private fun Instant.toAccesoTexto(): String =
    atZone(ZoneId.systemDefault()).format(accesoFormatter)

// ── Datos para previews ──────────────────────────────────────────────────────

private fun usuarioFake(
    uid: String = "u1",
    nombre: String = "Brian Tzuc",
    email: String = "brian@ejemplo.com",
    esAdmin: Boolean = false,
    activo: Boolean = true,
) = Usuario(
    uid          = uid,
    nombre       = nombre,
    email        = email,
    fotoUrl      = null,
    esAdmin      = esAdmin,
    activo       = activo,
    creadoEn     = Instant.now(),
    ultimoAcceso = Instant.now().minusSeconds(3600),
)

@Preview(showBackground = true, name = "Admin usuarios — tarjeta normal")
@Composable
private fun UsuarioCardNormalPreview() {
    QuinielaTheme {
        UsuarioCard(
            usuario        = usuarioFake(),
            onToggleAdmin  = {},
            onToggleActivo = {},
        )
    }
}

@Preview(showBackground = true, name = "Admin usuarios — tarjeta admin")
@Composable
private fun UsuarioCardAdminPreview() {
    QuinielaTheme {
        UsuarioCard(
            usuario        = usuarioFake("u2", "Ana García", "ana@ejemplo.com", esAdmin = true),
            onToggleAdmin  = {},
            onToggleActivo = {},
        )
    }
}

@Preview(showBackground = true, name = "Admin usuarios — tarjeta deshabilitado")
@Composable
private fun UsuarioCardDeshabilitadoPreview() {
    QuinielaTheme {
        UsuarioCard(
            usuario        = usuarioFake("u3", "Carlos López", "carlos@ejemplo.com", activo = false),
            onToggleAdmin  = {},
            onToggleActivo = {},
        )
    }
}

@Preview(showBackground = true, name = "Admin usuarios — lista completa")
@Composable
private fun ListaUsuariosPreview() {
    QuinielaTheme {
        ListaUsuarios(
            usuarios = listOf(
                usuarioFake("u1", "Brian Tzuc",   "brian@e.com", esAdmin = true),
                usuarioFake("u2", "Ana García",   "ana@e.com"),
                usuarioFake("u3", "Carlos López", "carlos@e.com", activo = false),
                usuarioFake("u4", "María Sánchez","maria@e.com"),
            ),
            onToggleAdmin  = {},
            onToggleActivo = {},
        )
    }
}
