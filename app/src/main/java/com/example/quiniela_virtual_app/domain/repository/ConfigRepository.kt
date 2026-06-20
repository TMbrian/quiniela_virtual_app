package com.example.quiniela_virtual_app.domain.repository

import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    fun observarConfig(): Flow<ConfiguracionApp?>
    suspend fun guardarConfig(config: ConfiguracionApp): Result<Unit>
}
