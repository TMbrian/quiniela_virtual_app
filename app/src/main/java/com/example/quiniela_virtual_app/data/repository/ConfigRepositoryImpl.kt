package com.example.quiniela_virtual_app.data.repository

import com.example.quiniela_virtual_app.data.dto.toDomain
import com.example.quiniela_virtual_app.data.source.firestore.ConfigFirestoreSource
import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import com.example.quiniela_virtual_app.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val source: ConfigFirestoreSource,
) : ConfigRepository {

    override fun observarConfig(): Flow<ConfiguracionApp?> =
        source.observar().map { it?.toDomain() }

    override suspend fun guardarConfig(config: ConfiguracionApp): Result<Unit> =
        source.guardar(config)
}
