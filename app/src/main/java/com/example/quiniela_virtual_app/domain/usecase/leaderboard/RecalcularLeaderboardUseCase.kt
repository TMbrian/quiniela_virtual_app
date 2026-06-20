package com.example.quiniela_virtual_app.domain.usecase.leaderboard

import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import javax.inject.Inject

class RecalcularLeaderboardUseCase @Inject constructor(
    private val repository: LeaderboardRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.recalcular()
}
