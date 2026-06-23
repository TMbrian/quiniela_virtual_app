package com.example.quiniela_virtual_app.presentation.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiniela_virtual_app.domain.model.PosicionGrupo
import com.example.quiniela_virtual_app.domain.usecase.partido.CalcularTablaGruposUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GruposViewModel @Inject constructor(
    calcularTabla: CalcularTablaGruposUseCase,
) : ViewModel() {

    val tablas: StateFlow<UiState<Map<String, List<PosicionGrupo>>>> =
        flujoTablas(calcularTabla)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun flujoTablas(
        calcularTabla: CalcularTablaGruposUseCase,
    ): Flow<UiState<Map<String, List<PosicionGrupo>>>> =
        calcularTabla()
            .map { UiState.Success(it) }
            .catch { e -> emit(UiState.Error(e.message ?: "Error al calcular tablas")) }
}
