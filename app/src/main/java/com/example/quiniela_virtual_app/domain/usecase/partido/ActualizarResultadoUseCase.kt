package com.example.quiniela_virtual_app.domain.usecase.partido

import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import javax.inject.Inject

class ActualizarResultadoUseCase @Inject constructor(
    private val partidoRepository: PartidoRepository,
    private val leaderboardRepository: LeaderboardRepository,
) {
    suspend operator fun invoke(
        partidoId: String,
        golesLocal: Int,
        golesVisitante: Int,
    ): Result<Unit> {
        val resultado = partidoRepository.actualizarResultado(partidoId, golesLocal, golesVisitante)
        if (resultado.isFailure) return resultado
        return leaderboardRepository.recalcular()
    }
}
