package com.example.quiniela_virtual_app.presentation.partidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.domain.repository.ConfigRepository
import com.example.quiniela_virtual_app.domain.usecase.prediccion.CalcularEstadoPrediccionUseCase
import com.example.quiniela_virtual_app.domain.usecase.prediccion.GuardarPrediccionUseCase
import com.example.quiniela_virtual_app.domain.usecase.prediccion.ObtenerPrediccionesUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.ObtenerPartidosUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class PartidoConPrediccion(
    val partido: Partido,
    val prediccion: Prediccion?,
    val estadoPrediccion: EstadoPrediccion,
)

data class PartidosData(
    val partidos: List<PartidoConPrediccion>,
    val lockAnticipacionMs: Long,
)

@HiltViewModel
class PartidosViewModel @Inject constructor(
    private val obtenerPartidos: ObtenerPartidosUseCase,
    private val obtenerPredicciones: ObtenerPrediccionesUseCase,
    private val guardarPrediccionUC: GuardarPrediccionUseCase,
    private val calcularEstado: CalcularEstadoPrediccionUseCase,
    private val configRepository: ConfigRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<PartidosData>>(UiState.Loading)
    val uiState: StateFlow<UiState<PartidosData>> = _uiState.asStateFlow()

    private val _guardarError = MutableStateFlow<String?>(null)
    val guardarError: StateFlow<String?> = _guardarError.asStateFlow()

    init { cargar() }

    fun limpiarError() { _guardarError.value = null }

    fun guardarPrediccion(partido: Partido, golesLocal: Int, golesVisitante: Int) {
        val uid = auth.currentUser?.uid ?: return
        val lockMs = lockMsActual() ?: return
        viewModelScope.launch {
            val prediccion = buildPrediccion(uid, partido.id, golesLocal, golesVisitante)
            guardarPrediccionUC(prediccion, partido, lockMs).onFailure { e ->
                _guardarError.value = e.message ?: "Error al guardar predicción"
            }
        }
    }

    private fun lockMsActual(): Long? =
        (_uiState.value as? UiState.Success)?.data?.lockAnticipacionMs

    private fun buildPrediccion(uid: String, partidoId: String, gL: Int, gV: Int): Prediccion {
        val ahora = Instant.now()
        return Prediccion(
            uid = uid, partidoId = partidoId,
            golesLocal = gL, golesVisitante = gV,
            creadaEn = ahora, actualizadaEn = ahora,
        )
    }

    private fun cargar() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            combine(
                obtenerPartidos(),
                obtenerPredicciones(uid),
                configRepository.observarConfig(),
            ) { partidos, predicciones, config -> combinarDatos(partidos, predicciones, config) }
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Error al cargar") }
                .collect { data -> _uiState.value = UiState.Success(data) }
        }
    }

    private fun combinarDatos(
        partidos: List<Partido>,
        predicciones: List<Prediccion>,
        config: ConfiguracionApp?,
    ): PartidosData {
        val predMap = predicciones.associateBy { it.partidoId }
        val lockMs = (config?.lockAnticipacionMin ?: 60).toLong() * 60_000L
        val ahora = System.currentTimeMillis()
        val items = partidos
            .filter { !it.excluido }
            .map { p -> PartidoConPrediccion(p, predMap[p.id], calcularEstado(p, ahora, lockMs)) }
        return PartidosData(items, lockMs)
    }
}
