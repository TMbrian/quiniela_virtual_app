package com.example.quiniela_virtual_app.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.domain.usecase.partido.ObtenerPartidosUseCase
import com.example.quiniela_virtual_app.domain.usecase.prediccion.ObtenerPrediccionesUseCase
import com.example.quiniela_virtual_app.domain.usecase.usuario.ObtenerUsuarioUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerfilData(
    val usuario: Usuario,
    val puesto: Int?,
    val stats: PosicionLeaderboard?,
    val rachaActual: Int = 0,
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val obtenerUsuario: ObtenerUsuarioUseCase,
    private val leaderboardRepository: LeaderboardRepository,
    private val obtenerPartidos: ObtenerPartidosUseCase,
    private val obtenerPredicciones: ObtenerPrediccionesUseCase,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<PerfilData>>(UiState.Loading)
    val uiState: StateFlow<UiState<PerfilData>> = _uiState.asStateFlow()

    init { cargar() }

    private fun cargar() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            combine(
                obtenerUsuario(),
                leaderboardRepository.observarLeaderboard(),
                obtenerPartidos(),
                obtenerPredicciones(uid),
            ) { usuario, leaderboard, partidos, predicciones ->
                construirPerfilData(usuario, leaderboard, partidos, predicciones)
            }
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Error al cargar perfil") }
                .collect { estado -> _uiState.value = estado }
        }
    }

    private fun construirPerfilData(
        usuario: Usuario?,
        leaderboard: List<PosicionLeaderboard>,
        partidos: List<Partido>,
        predicciones: List<Prediccion>,
    ): UiState<PerfilData> {
        if (usuario == null) return UiState.Error("Usuario no encontrado")
        val indice = leaderboard.indexOfFirst { it.uid == usuario.uid }
        val stats = leaderboard.getOrNull(indice)
        val puesto = if (indice >= 0) indice + 1 else null
        return UiState.Success(PerfilData(usuario, puesto, stats, calcularRacha(partidos, predicciones)))
    }

    private fun calcularRacha(partidos: List<Partido>, predicciones: List<Prediccion>): Int {
        val predMap = predicciones.associateBy { it.partidoId }
        val finalizados = partidos
            .filter { it.estado == EstadoPartido.FINALIZADO && !it.excluido }
            .sortedByDescending { it.fecha }
        var racha = 0
        for (partido in finalizados) {
            val gL = partido.golesLocal ?: break
            val gV = partido.golesVisitante ?: break
            val pred = predMap[partido.id] ?: break
            if (!esResultadoCorrecto(gL, gV, pred.golesLocal, pred.golesVisitante)) break
            racha++
        }
        return racha
    }

    private fun esResultadoCorrecto(gL: Int, gV: Int, pL: Int, pV: Int): Boolean =
        (gL > gV && pL > pV) || (gL < gV && pL < pV) || (gL == gV && pL == pV)
}
