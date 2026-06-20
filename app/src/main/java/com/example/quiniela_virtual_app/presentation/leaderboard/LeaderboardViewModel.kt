package com.example.quiniela_virtual_app.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.presentation.shared.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<PosicionLeaderboard>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<PosicionLeaderboard>>> = _uiState.asStateFlow()

    init { cargar() }

    private fun cargar() {
        viewModelScope.launch {
            repository.observarLeaderboard()
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Error al cargar posiciones") }
                .collect { posiciones -> _uiState.value = UiState.Success(posiciones) }
        }
    }
}
