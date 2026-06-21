package com.example.quiniela_virtual_app.domain.repository

import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import kotlinx.coroutines.flow.Flow

interface PartidoRepository {
    fun observarPartidos(): Flow<List<Partido>>
    suspend fun sincronizarDesdeApi(codigoApi: String): Result<Int>
    suspend fun actualizarResultado(partidoId: String, golesLocal: Int, golesVisitante: Int): Result<Unit>
    suspend fun cambiarEstado(partidoId: String, estado: EstadoPartido): Result<Unit>
    suspend fun setLockOverride(partidoId: String, unlock: Boolean, expiryMs: Long?): Result<Unit>
    suspend fun setExcluido(partidoId: String, excluido: Boolean): Result<Unit>
    suspend fun restablecerTodos(): Result<Unit>
}
