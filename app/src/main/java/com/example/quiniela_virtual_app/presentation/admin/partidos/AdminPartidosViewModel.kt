package com.example.quiniela_virtual_app.presentation.admin.partidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.usecase.partido.ActualizarResultadoUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.CambiarEstadoUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.ObtenerPartidosUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.RestablecerPartidosUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.SetExcluidoUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.SetLockOverrideUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.SincronizarPartidosUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GrupoSeccion(val nombre: String, val partidos: List<Partido>)
data class JornadaSeccion(val titulo: String, val subtitulo: String, val grupos: List<GrupoSeccion>)

@HiltViewModel
class AdminPartidosViewModel @Inject constructor(
    private val obtenerPartidos: ObtenerPartidosUseCase,
    private val actualizarResultadoUC: ActualizarResultadoUseCase,
    private val sincronizarUC: SincronizarPartidosUseCase,
    private val restablecerUC: RestablecerPartidosUseCase,
    private val cambiarEstadoUC: CambiarEstadoUseCase,
    private val setExcluidoUC: SetExcluidoUseCase,
    private val setLockOverrideUC: SetLockOverrideUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Partido>>>(UiState.Loading)

    private val _accionEstado = MutableStateFlow<String?>(null)
    val accionEstado: StateFlow<String?> = _accionEstado.asStateFlow()

    private val _cargandoAccion = MutableStateFlow(false)
    val cargandoAccion: StateFlow<Boolean> = _cargandoAccion.asStateFlow()

    private val _filtroEstado = MutableStateFlow<EstadoPartido?>(null)
    val filtroEstado: StateFlow<EstadoPartido?> = _filtroEstado.asStateFlow()

    val secciones: StateFlow<UiState<List<JornadaSeccion>>> =
        combine(_uiState, _filtroEstado) { estado, filtro ->
            when (estado) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error   -> UiState.Error(estado.mensaje)
                is UiState.Success -> UiState.Success(agruparEnSecciones(estado.data, filtro))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { cargar() }

    fun limpiarMensaje() { _accionEstado.value = null }

    fun filtrarPor(estado: EstadoPartido?) { _filtroEstado.value = estado }

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

    fun cambiarEstado(partido: Partido, nuevoEstado: EstadoPartido) = ejecutarAccion {
        cambiarEstadoUC(partido.id, nuevoEstado).fold(
            onSuccess = { _accionEstado.value = "Estado cambiado a ${etiquetaEstado(nuevoEstado)}" },
            onFailure = { e -> _accionEstado.value = e.message ?: "Error al cambiar estado" },
        )
    }

    fun abrirPredicciones(partido: Partido, duracionMs: Long) = ejecutarAccion {
        val expiryMs = System.currentTimeMillis() + duracionMs
        setLockOverrideUC(partido.id, unlock = true, expiryMs = expiryMs).fold(
            onSuccess = { _accionEstado.value = "Predicciones abiertas" },
            onFailure = { e -> _accionEstado.value = e.message ?: "Error al abrir" },
        )
    }

    fun forzarBloqueo(partido: Partido) = ejecutarAccion {
        setLockOverrideUC(partido.id, unlock = false, expiryMs = null).fold(
            onSuccess = { _accionEstado.value = "Predicciones bloqueadas" },
            onFailure = { e -> _accionEstado.value = e.message ?: "Error al bloquear" },
        )
    }

    fun toggleExcluido(partido: Partido) = ejecutarAccion {
        setExcluidoUC(partido.id, !partido.excluido).fold(
            onSuccess = { _accionEstado.value = if (partido.excluido) "Partido incluido" else "Partido excluido" },
            onFailure = { e -> _accionEstado.value = e.message ?: "Error al actualizar" },
        )
    }

    private fun etiquetaEstado(estado: EstadoPartido): String = when (estado) {
        EstadoPartido.PROGRAMADO -> "Programado"
        EstadoPartido.EN_VIVO    -> "En vivo"
        EstadoPartido.FINALIZADO -> "Finalizado"
        EstadoPartido.SUSPENDIDO -> "Suspendido"
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

    private fun agruparEnSecciones(partidos: List<Partido>, filtro: EstadoPartido?): List<JornadaSeccion> {
        val filtrados = filtrarPartidos(partidos, filtro)
        val (deGrupos, deEliminacion) = filtrados.partition { it.fase == FASE_GRUPOS }
        return seccionesGrupos(deGrupos) + seccionesEliminacion(deEliminacion)
    }

    private fun seccionesGrupos(partidos: List<Partido>): List<JornadaSeccion> =
        partidos
            .groupBy { it.jornada }
            .entries
            .sortedBy { it.key }
            .map { (jornada, porJornada) ->
                JornadaSeccion(
                    titulo = "Jornada $jornada",
                    subtitulo = FASE_GRUPOS,
                    grupos = agruparPorGrupo(porJornada),
                )
            }

    private fun seccionesEliminacion(partidos: List<Partido>): List<JornadaSeccion> =
        partidos
            .groupBy { it.fase }
            .entries
            .sortedBy { ordenFase(it.key) }
            .map { (fase, porFase) ->
                JornadaSeccion(
                    titulo = fase,
                    subtitulo = "",
                    grupos = listOf(GrupoSeccion("", porFase.sortedBy { it.fecha })),
                )
            }

    private fun filtrarPartidos(partidos: List<Partido>, filtro: EstadoPartido?): List<Partido> {
        if (filtro == null) return partidos
        return partidos.filter { it.estado == filtro }
    }

    private fun agruparPorGrupo(partidos: List<Partido>): List<GrupoSeccion> =
        partidos
            .groupBy { it.grupo }
            .entries
            .sortedBy { it.key }
            .map { (grupo, lista) ->
                GrupoSeccion(nombre = grupo, partidos = lista.sortedBy { it.fecha })
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
