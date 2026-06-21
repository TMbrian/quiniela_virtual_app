package com.example.quiniela_virtual_app.data.repository

import com.example.quiniela_virtual_app.data.dto.toDomain
import com.example.quiniela_virtual_app.data.remote.api.FootballApiService
import com.example.quiniela_virtual_app.data.remote.dto.toDomain
import com.example.quiniela_virtual_app.data.source.firestore.PartidoFirestoreSource
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartidoRepositoryImpl @Inject constructor(
    private val source: PartidoFirestoreSource,
    private val apiService: FootballApiService,
) : PartidoRepository {

    override fun observarPartidos(): Flow<List<Partido>> =
        source.observar().map { lista -> lista.map { it.toDomain() } }

    override suspend fun sincronizarDesdeApi(codigoApi: String): Result<Int> = runCatching {
        val partidos = apiService.obtenerPartidos(codigoApi).matches
            .filter { it.id != null }
            .map { it.toDomain() }
        source.sincronizar(partidos).getOrThrow()
        partidos.size
    }

    override suspend fun actualizarResultado(
        partidoId: String,
        golesLocal: Int,
        golesVisitante: Int,
    ): Result<Unit> = source.actualizarResultado(partidoId, golesLocal, golesVisitante)

    override suspend fun cambiarEstado(partidoId: String, estado: EstadoPartido): Result<Unit> =
        source.cambiarEstado(partidoId, estado)

    override suspend fun setLockOverride(
        partidoId: String,
        unlock: Boolean,
        expiryMs: Long?,
    ): Result<Unit> = source.setLockOverride(partidoId, unlock, expiryMs)

    override suspend fun setExcluido(partidoId: String, excluido: Boolean): Result<Unit> =
        source.setExcluido(partidoId, excluido)

    override suspend fun restablecerTodos(): Result<Unit> =
        source.restablecerTodos()
}
