package com.example.quiniela_virtual_app.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.usecase.usuario.ObtenerUsuarioUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val obtenerUsuario: ObtenerUsuarioUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Usuario>>(UiState.Loading)
    val uiState: StateFlow<UiState<Usuario>> = _uiState.asStateFlow()

    init { cargar() }

    private fun cargar() {
        viewModelScope.launch {
            obtenerUsuario()
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Error al cargar perfil") }
                .collect { usuario ->
                    _uiState.value = if (usuario != null) UiState.Success(usuario)
                                     else UiState.Error("Usuario no encontrado")
                }
        }
    }
}
