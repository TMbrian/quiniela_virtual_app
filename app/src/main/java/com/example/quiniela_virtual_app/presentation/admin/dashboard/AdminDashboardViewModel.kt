package com.example.quiniela_virtual_app.presentation.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import com.example.quiniela_virtual_app.domain.repository.UsuarioRepository
import com.example.quiniela_virtual_app.presentation.shared.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardStats(
    val totalPartidos: Int,
    val partidosProgramados: Int,
    val partidosEnVivo: Int,
    val partidosFinalizados: Int,
    val totalUsuarios: Int,
    val usuariosActivos: Int,
    val lider: PosicionLeaderboard?,
    val totalPredicciones: Int,
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val partidoRepository: PartidoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val leaderboardRepository: LeaderboardRepository,
) : ViewModel() {

    val stats: StateFlow<UiState<DashboardStats>> =
        combine(
            partidoRepository.observarPartidos(),
            usuarioRepository.observarUsuarios(),
            leaderboardRepository.observarLeaderboard(),
        ) { partidos, usuarios, leaderboard ->
            UiState.Success(calcularStats(partidos, usuarios, leaderboard)) as UiState<DashboardStats>
        }
        .catch { e -> emit(UiState.Error(e.message ?: "Error al cargar estadísticas")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun calcularStats(
        partidos: List<Partido>,
        usuarios: List<Usuario>,
        leaderboard: List<PosicionLeaderboard>,
    ): DashboardStats {
        val porEstado = partidos.groupBy { it.estado }
        return DashboardStats(
            totalPartidos       = partidos.size,
            partidosProgramados = porEstado[EstadoPartido.PROGRAMADO]?.size ?: 0,
            partidosEnVivo      = porEstado[EstadoPartido.EN_VIVO]?.size ?: 0,
            partidosFinalizados = porEstado[EstadoPartido.FINALIZADO]?.size ?: 0,
            totalUsuarios       = usuarios.size,
            usuariosActivos     = usuarios.count { it.activo },
            lider               = leaderboard.maxByOrNull { it.puntosTotales },
            totalPredicciones   = leaderboard.sumOf { it.prediccionesTotales },
        )
    }
}
