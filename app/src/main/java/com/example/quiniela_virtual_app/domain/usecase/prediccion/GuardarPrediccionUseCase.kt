package com.example.quiniela_virtual_app.domain.usecase.prediccion

import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.domain.repository.PrediccionRepository
import javax.inject.Inject

class GuardarPrediccionUseCase @Inject constructor(
    private val repository: PrediccionRepository,
    private val calcularEstado: CalcularEstadoPrediccionUseCase,
) {
    suspend operator fun invoke(
        prediccion: Prediccion,
        partido: Partido,
        lockAnticipacionMs: Long,
    ): Result<Unit> {
        val estado = calcularEstado(partido, System.currentTimeMillis(), lockAnticipacionMs)
        if (estado == EstadoPrediccion.BLOQUEADO) {
            return Result.failure(IllegalStateException("El partido está bloqueado para predicciones"))
        }
        return repository.guardarPrediccion(prediccion)
    }
}
