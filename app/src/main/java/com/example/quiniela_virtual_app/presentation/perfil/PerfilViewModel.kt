package com.example.quiniela_virtual_app.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.domain.usecase.usuario.ObtenerUsuarioUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
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
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val obtenerUsuario: ObtenerUsuarioUseCase,
    private val leaderboardRepository: LeaderboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<PerfilData>>(UiState.Loading)
    val uiState: StateFlow<UiState<PerfilData>> = _uiState.asStateFlow()

    init { cargar() }

    private fun cargar() {
        viewModelScope.launch {
            combine(obtenerUsuario(), leaderboardRepository.observarLeaderboard()) { usuario, leaderboard ->
                construirPerfilData(usuario, leaderboard)
            }
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Error al cargar perfil") }
                .collect { estado -> _uiState.value = estado }
        }
    }

    private fun construirPerfilData(usuario: Usuario?, leaderboard: List<PosicionLeaderboard>): UiState<PerfilData> {
        if (usuario == null) return UiState.Error("Usuario no encontrado")
        val indice = leaderboard.indexOfFirst { it.uid == usuario.uid }
        val stats = leaderboard.getOrNull(indice)
        val puesto = if (indice >= 0) indice + 1 else null
        return UiState.Success(PerfilData(usuario, puesto, stats))
    }
}
