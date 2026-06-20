package com.example.quiniela_virtual_app.presentation.auth

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme

/** Agrupa los campos del formulario para mantener [LoginContent] con ≤ 7 parámetros. */
private data class FormularioLogin(
    val email: String,
    val password: String,
    val nombre: String,
    val modoLogin: Boolean,
)

/** Pantalla pública: conecta él [LoginViewModel] con la UI stateless [LoginContent]. */
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val loginState by viewModel.loginState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var modoLogin by remember { mutableStateOf(true) }

    LoginContent(
        loginState = loginState,
        formulario = FormularioLogin(email, password, nombre, modoLogin),
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onNombreChange = { nombre = it },
        onModoChange = { modoLogin = !modoLogin; viewModel.resetState() },
        onAccion = { dispararAccion(modoLogin, email, password, nombre, viewModel) },
    )
}

/** UI stateless del formulario de auth; recibe estado y callbacks sin depender del ViewModel. */
@Composable
private fun LoginContent(
    loginState: UiState<Unit>,
    formulario: FormularioLogin,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onModoChange: () -> Unit,
    onAccion: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LogoQuiniela()
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Quiniela Virtual",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Predice · Compite · Gana",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (formulario.modoLogin) "Iniciar sesión" else "Crear cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!formulario.modoLogin) {
                    OutlinedTextField(
                        value = formulario.nombre,
                        onValueChange = onNombreChange,
                        label = { Text("Nombre completo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                OutlinedTextField(
                    value = formulario.email,
                    onValueChange = onEmailChange,
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                OutlinedTextField(
                    value = formulario.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )

                if (loginState is UiState.Error) {
                    Text(
                        text = loginState.mensaje,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = onAccion,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loginState !is UiState.Loading,
                ) {
                    ContenidoBotonAccion(loginState, formulario.modoLogin)
                }
            }
        }

        TextButton(onClick = onModoChange) {
            Text(
                text = if (formulario.modoLogin) "¿No tienes cuenta? Regístrate"
                       else "¿Ya tienes cuenta? Inicia sesión",
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Ícono circular con balón de fútbol que representa la app en la pantalla de login. */
@Composable
private fun LogoQuiniela() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "⚽", fontSize = 52.sp)
    }
}

/** Delega la acción del botón al ViewModel según el modo actual (login o registro). */
private fun dispararAccion(
    modoLogin: Boolean,
    email: String,
    password: String,
    nombre: String,
    viewModel: LoginViewModel,
) {
    if (modoLogin) viewModel.iniciarSesion(email, password)
    else viewModel.registrar(email, password, nombre)
}

/** Muestra spinner mientras carga o el texto del botón cuando está disponible. */
@Composable
private fun ContenidoBotonAccion(loginState: UiState<Unit>, modoLogin: Boolean) {
    if (loginState is UiState.Loading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
    } else {
        Text(if (modoLogin) "Entrar" else "Registrarse")
    }
}

@Preview(showBackground = true, name = "Login — modo claro")
@Composable
private fun LoginPreview() {
    QuinielaTheme {
        LoginContent(
            loginState = UiState.Success(Unit),
            formulario = FormularioLogin("brian@ejemplo.com", "••••••", "", true),
            onEmailChange = {}, onPasswordChange = {}, onNombreChange = {},
            onModoChange = {}, onAccion = {},
        )
    }
}

@Preview(showBackground = true, name = "Login — modo oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginOscuroPreview() {
    QuinielaTheme(darkTheme = true) {
        LoginContent(
            loginState = UiState.Success(Unit),
            formulario = FormularioLogin("brian@ejemplo.com", "••••••", "", true),
            onEmailChange = {}, onPasswordChange = {}, onNombreChange = {},
            onModoChange = {}, onAccion = {},
        )
    }
}

@Preview(showBackground = true, name = "Login — registro")
@Composable
private fun RegistroPreview() {
    QuinielaTheme {
        LoginContent(
            loginState = UiState.Success(Unit),
            formulario = FormularioLogin("", "", "Brian Tzuc", false),
            onEmailChange = {}, onPasswordChange = {}, onNombreChange = {},
            onModoChange = {}, onAccion = {},
        )
    }
}
