package com.example.quiniela_virtual_app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.UsuarioRepository
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    val estaAutenticado: StateFlow<Boolean> = observarAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUser != null)

    val usuarioActual: StateFlow<Usuario?> = usuarioRepository.observarUsuarioActual()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun iniciarSesion(email: String, password: String) {
        _loginState.value = UiState.Loading
        viewModelScope.launch {
            runCatching { auth.signInWithEmailAndPassword(email, password).await() }
                .fold(
                    onSuccess = { _loginState.value = UiState.Success(Unit) },
                    onFailure = { e -> _loginState.value = UiState.Error(mensajeError(e)) },
                )
        }
    }

    fun registrar(email: String, password: String, nombre: String) {
        _loginState.value = UiState.Loading
        viewModelScope.launch {
            runCatching {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: error("UID nulo tras registro")
                crearPerfil(uid, nombre, email)
            }.fold(
                onSuccess = { _loginState.value = UiState.Success(Unit) },
                onFailure = { e -> _loginState.value = UiState.Error(mensajeError(e)) },
            )
        }
    }

    fun cerrarSesion() = auth.signOut()

    fun resetState() {
        _loginState.value = UiState.Success(Unit)
    }

    private suspend fun crearPerfil(uid: String, nombre: String, email: String) {
        val ahora = Instant.now()
        val usuario = Usuario(
            uid = uid, nombre = nombre, email = email, fotoUrl = null,
            esAdmin = false, activo = true, creadoEn = ahora, ultimoAcceso = ahora,
        )
        usuarioRepository.crearPerfil(usuario).getOrThrow()
    }

    private fun mensajeError(e: Throwable): String = when {
        e.message?.contains("email address is already in use") == true ->
            "Este email ya está registrado"
        e.message?.contains("password is invalid") == true ->
            "Contraseña incorrecta"
        e.message?.contains("no user record") == true ->
            "No existe una cuenta con ese email"
        e.message?.contains("badly formatted") == true ->
            "El formato del email no es válido"
        else -> e.message ?: "Error desconocido"
    }

    private fun observarAuthState(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
