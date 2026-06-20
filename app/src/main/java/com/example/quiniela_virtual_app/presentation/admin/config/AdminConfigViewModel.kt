package com.example.quiniela_virtual_app.presentation.admin.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import com.example.quiniela_virtual_app.domain.repository.ConfigRepository
import com.example.quiniela_virtual_app.presentation.shared.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AdminConfigViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ConfiguracionApp>>(UiState.Loading)
    val uiState: StateFlow<UiState<ConfiguracionApp>> = _uiState.asStateFlow()

    private val _guardadoEstado = MutableStateFlow<String?>(null)
    val guardadoEstado: StateFlow<String?> = _guardadoEstado.asStateFlow()

    init { cargar() }

    fun limpiarMensaje() { _guardadoEstado.value = null }

    fun guardar(
        nombreCompeticion: String,
        descripcion: String,
        temporada: String,
        puntosExacto: Int,
        puntosTendencia: Int,
        lockAnticipacionMin: Int,
        activa: Boolean,
    ) {
        val actual = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val config = buildConfig(actual, nombreCompeticion, descripcion, temporada,
                puntosExacto, puntosTendencia, lockAnticipacionMin, activa)
            configRepository.guardarConfig(config).fold(
                onSuccess = { _guardadoEstado.value = "Configuración guardada" },
                onFailure = { e -> _guardadoEstado.value = e.message ?: "Error al guardar" },
            )
        }
    }

    private fun buildConfig(
        actual: ConfiguracionApp,
        nombre: String,
        descripcion: String,
        temporada: String,
        puntosExacto: Int,
        puntosTendencia: Int,
        lockMin: Int,
        activa: Boolean,
    ) = actual.copy(
        nombreCompeticion = nombre,
        descripcion = descripcion,
        temporada = temporada,
        puntosExacto = puntosExacto,
        puntosTendencia = puntosTendencia,
        lockAnticipacionMin = lockMin,
        activa = activa,
        actualizadaEn = Instant.now(),
    )

    private fun cargar() {
        viewModelScope.launch {
            configRepository.observarConfig()
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Error al cargar config") }
                .collect { config ->
                    _uiState.value = if (config != null) UiState.Success(config)
                                     else UiState.Error("No hay configuración. El admin debe crearla.")
                }
        }
    }
}
