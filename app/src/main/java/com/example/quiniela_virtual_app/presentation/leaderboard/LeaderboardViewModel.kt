package com.example.quiniela_virtual_app.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    val currentUid: String? get() = auth.currentUser?.uid

    private val _rawLeaderboard = MutableStateFlow<UiState<List<PosicionLeaderboard>>>(UiState.Loading)

    private val _filtroJornada = MutableStateFlow<String?>(null)
    val filtroJornada: StateFlow<String?> = _filtroJornada.asStateFlow()

    val jornadasDisponibles: StateFlow<List<String>> =
        _rawLeaderboard.map { estado ->
            if (estado !is UiState.Success) return@map emptyList()
            estado.data
                .flatMap { it.puntosPorJornada.keys }
                .distinct()
                .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<UiState<List<PosicionLeaderboard>>> =
        combine(_rawLeaderboard, _filtroJornada) { estado, jornada ->
            if (estado !is UiState.Success || jornada == null) return@combine estado
            UiState.Success(estado.data.sortedByDescending { it.puntosPorJornada[jornada] ?: 0 })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun filtrarJornada(jornada: String?) { _filtroJornada.value = jornada }

    init { cargar() }

    private fun cargar() {
        viewModelScope.launch {
            repository.observarLeaderboard()
                .catch { e -> _rawLeaderboard.value = UiState.Error(e.message ?: "Error al cargar posiciones") }
                .collect { posiciones -> _rawLeaderboard.value = UiState.Success(posiciones) }
        }
    }
}
