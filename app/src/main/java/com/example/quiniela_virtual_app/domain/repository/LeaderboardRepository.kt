package com.example.quiniela_virtual_app.domain.repository

import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import kotlinx.coroutines.flow.Flow

interface LeaderboardRepository {
    fun observarLeaderboard(): Flow<List<PosicionLeaderboard>>
    suspend fun recalcular(): Result<Unit>
}
