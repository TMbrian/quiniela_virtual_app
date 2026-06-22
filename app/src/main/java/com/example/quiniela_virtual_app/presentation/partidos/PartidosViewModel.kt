package com.example.quiniela_virtual_app.presentation.partidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class PartidoConPrediccion(
    val partido: Partido,
    val prediccion: Prediccion?,
    val estadoPrediccion: EstadoPrediccion,
    val puntosGanados: Int?,
)

data class PartidosData(
    val partidos: List<PartidoConPrediccion>,
    val lockAnticipacionMs: Long,
)

data class PartidoGrupoSeccion(val nombre: String, val items: List<PartidoConPrediccion>)
data class PartidoJornadaSeccion(val titulo: String, val subtitulo: String, val grupos: List<PartidoGrupoSeccion>)

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

    private val _mensajeExito = MutableStateFlow<String?>(null)
    val mensajeExito: StateFlow<String?> = _mensajeExito.asStateFlow()

    private val _filtroEstado = MutableStateFlow<EstadoPartido?>(null)
    val filtroEstado: StateFlow<EstadoPartido?> = _filtroEstado.asStateFlow()

    val secciones: StateFlow<UiState<List<PartidoJornadaSeccion>>> =
        combine(_uiState, _filtroEstado) { estado, filtro ->
            when (estado) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error   -> UiState.Error(estado.mensaje)
                is UiState.Success -> UiState.Success(agruparEnSecciones(estado.data.partidos, filtro))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { cargar() }

    fun limpiarError() { _guardarError.value = null }
    fun limpiarMensajeExito() { _mensajeExito.value = null }

    fun filtrarPor(estado: EstadoPartido?) { _filtroEstado.value = estado }

    fun guardarPrediccion(partido: Partido, golesLocal: Int, golesVisitante: Int) {
        val uid = auth.currentUser?.uid ?: return
        val lockMs = lockMsActual() ?: return
        viewModelScope.launch {
            val prediccion = buildPrediccion(uid, partido.id, golesLocal, golesVisitante)
            guardarPrediccionUC(prediccion, partido, lockMs).fold(
                onSuccess = { onPrediccionGuardada() },
                onFailure = { e -> _guardarError.value = e.message ?: "Error al guardar predicción" },
            )
        }
    }

    private fun onPrediccionGuardada() {
        _mensajeExito.value = "Predicción guardada"
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
        val puntosExacto = config?.puntosExacto ?: 5
        val puntosTendencia = config?.puntosTendencia ?: 3
        val ahora = System.currentTimeMillis()
        val items = partidos
            .filter { !it.excluido }
            .map { p ->
                val pred = predMap[p.id]
                PartidoConPrediccion(p, pred, calcularEstado(p, ahora, lockMs), calcularPuntos(p, pred, puntosExacto, puntosTendencia))
            }
        return PartidosData(items, lockMs)
    }

    private fun calcularPuntos(partido: Partido, prediccion: Prediccion?, puntosExacto: Int, puntosTendencia: Int): Int? {
        if (partido.estado != EstadoPartido.FINALIZADO) return null
        if (prediccion == null) return null
        val gL = partido.golesLocal ?: return null
        val gV = partido.golesVisitante ?: return null
        return when {
            gL == prediccion.golesLocal && gV == prediccion.golesVisitante -> puntosExacto
            mismoGanador(gL, gV, prediccion.golesLocal, prediccion.golesVisitante) -> puntosTendencia
            else -> 0
        }
    }

    private fun mismoGanador(gL: Int, gV: Int, pL: Int, pV: Int): Boolean =
        (gL > gV && pL > pV) || (gL < gV && pL < pV) || (gL == gV && pL == pV)

    private fun agruparEnSecciones(items: List<PartidoConPrediccion>, filtro: EstadoPartido?): List<PartidoJornadaSeccion> {
        val filtrados = filtrarItems(items, filtro)
        val (deGrupos, deEliminacion) = filtrados.partition { it.partido.fase == FASE_GRUPOS }
        return seccionesGrupos(deGrupos) + seccionesEliminacion(deEliminacion)
    }

    private fun filtrarItems(items: List<PartidoConPrediccion>, filtro: EstadoPartido?): List<PartidoConPrediccion> {
        if (filtro == null) return items
        return items.filter { it.partido.estado == filtro }
    }

    private fun seccionesGrupos(items: List<PartidoConPrediccion>): List<PartidoJornadaSeccion> =
        items
            .groupBy { it.partido.jornada }
            .entries
            .sortedBy { it.key }
            .map { (jornada, porJornada) ->
                PartidoJornadaSeccion(
                    titulo = "Jornada $jornada",
                    subtitulo = FASE_GRUPOS,
                    grupos = agruparPorGrupo(porJornada),
                )
            }

    private fun seccionesEliminacion(items: List<PartidoConPrediccion>): List<PartidoJornadaSeccion> =
        items
            .groupBy { it.partido.fase }
            .entries
            .sortedBy { ordenFase(it.key) }
            .map { (fase, porFase) ->
                PartidoJornadaSeccion(
                    titulo = fase,
                    subtitulo = "",
                    grupos = listOf(PartidoGrupoSeccion("", porFase.sortedBy { it.partido.fecha })),
                )
            }

    private fun agruparPorGrupo(items: List<PartidoConPrediccion>): List<PartidoGrupoSeccion> =
        items
            .groupBy { it.partido.grupo }
            .entries
            .sortedBy { it.key }
            .map { (grupo, lista) ->
                PartidoGrupoSeccion(nombre = grupo, items = lista.sortedBy { it.partido.fecha })
            }

    private fun ordenFase(fase: String): Int = when (fase) {
        "Grupos"    -> 0
        "Octavos"   -> 1
        "Cuartos"   -> 2
        "Semifinal" -> 3
        "3er Lugar" -> 4
        "Final"     -> 5
        else        -> 99
    }

    companion object {
        private const val FASE_GRUPOS = "Grupos"
    }
}
