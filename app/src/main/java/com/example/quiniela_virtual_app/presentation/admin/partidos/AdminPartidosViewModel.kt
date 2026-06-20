package com.example.quiniela_virtual_app.presentation.admin.partidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.usecase.partido.ActualizarResultadoUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.ObtenerPartidosUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.RestablecerPartidosUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.SincronizarPartidosUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminPartidosViewModel @Inject constructor(
    private val obtenerPartidos: ObtenerPartidosUseCase,
    private val actualizarResultadoUC: ActualizarResultadoUseCase,
    private val sincronizarUC: SincronizarPartidosUseCase,
    private val restablecerUC: RestablecerPartidosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Partido>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Partido>>> = _uiState.asStateFlow()

    private val _accionEstado = MutableStateFlow<String?>(null)
    val accionEstado: StateFlow<String?> = _accionEstado.asStateFlow()

    private val _cargandoAccion = MutableStateFlow(false)
    val cargandoAccion: StateFlow<Boolean> = _cargandoAccion.asStateFlow()

    init { cargar() }

    fun limpiarMensaje() { _accionEstado.value = null }

    fun sincronizarDesdeApi() = ejecutarAccion {
        sincronizarUC().fold(
            onSuccess = { n -> _accionEstado.value = "Se sincronizaron $n partidos" },
            onFailure = { e -> _accionEstado.value = e.message ?: "Error al sincronizar" },
        )
    }

    fun restablecer() = ejecutarAccion {
        restablecerUC().fold(
            onSuccess = { _accionEstado.value = "Partidos restablecidos" },
            onFailure = { e -> _accionEstado.value = e.message ?: "Error al restablecer" },
        )
    }

    fun actualizarResultado(partido: Partido, golesLocal: Int, golesVisitante: Int) =
        ejecutarAccion {
            actualizarResultadoUC(partido.id, golesLocal, golesVisitante).fold(
                onSuccess = { _accionEstado.value = "Resultado actualizado" },
                onFailure = { e -> _accionEstado.value = e.message ?: "Error al actualizar" },
            )
        }

    private fun ejecutarAccion(bloque: suspend () -> Unit) {
        if (_cargandoAccion.value) return
        viewModelScope.launch {
            _cargandoAccion.value = true
            bloque()
            _cargandoAccion.value = false
        }
    }

    private fun cargar() {
        viewModelScope.launch {
            obtenerPartidos()
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Error al cargar") }
                .collect { partidos ->
                    _uiState.value = UiState.Success(partidos.sortedBy { it.fecha })
                }
        }
    }
}
