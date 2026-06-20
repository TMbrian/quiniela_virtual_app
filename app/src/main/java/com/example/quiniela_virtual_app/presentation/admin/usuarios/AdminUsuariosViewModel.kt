package com.example.quiniela_virtual_app.presentation.admin.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.UsuarioRepository
import com.example.quiniela_virtual_app.domain.usecase.usuario.CambiarRolUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminUsuariosViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val cambiarRol: CambiarRolUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Usuario>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Usuario>>> = _uiState.asStateFlow()

    private val _accionEstado = MutableStateFlow<String?>(null)
    val accionEstado: StateFlow<String?> = _accionEstado.asStateFlow()

    init { cargar() }

    fun limpiarMensaje() { _accionEstado.value = null }

    fun toggleAdmin(usuario: Usuario) {
        viewModelScope.launch {
            cambiarRol(usuario.uid, !usuario.esAdmin).fold(
                onSuccess = { _accionEstado.value = "Rol actualizado" },
                onFailure = { e -> _accionEstado.value = e.message ?: "Error al cambiar rol" },
            )
        }
    }

    fun toggleActivo(usuario: Usuario) {
        viewModelScope.launch {
            usuarioRepository.cambiarEstadoActivo(usuario.uid, !usuario.activo).fold(
                onSuccess = { _accionEstado.value = if (usuario.activo) "Usuario deshabilitado" else "Usuario habilitado" },
                onFailure = { e -> _accionEstado.value = e.message ?: "Error al cambiar estado" },
            )
        }
    }

    private fun cargar() {
        viewModelScope.launch {
            usuarioRepository.observarUsuarios()
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Error al cargar usuarios") }
                .collect { usuarios -> _uiState.value = UiState.Success(usuarios) }
        }
    }
}
