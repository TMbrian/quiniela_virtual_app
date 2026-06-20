package com.example.quiniela_virtual_app.data.repository

import com.example.quiniela_virtual_app.data.dto.ConfiguracionAppDto
import com.example.quiniela_virtual_app.data.dto.PartidoDto
import com.example.quiniela_virtual_app.data.dto.PrediccionDto
import com.example.quiniela_virtual_app.data.dto.PosicionLeaderboardDto
import com.example.quiniela_virtual_app.data.dto.UsuarioDto
import com.example.quiniela_virtual_app.data.dto.toDomain
import com.example.quiniela_virtual_app.data.source.firestore.ConfigFirestoreSource
import com.example.quiniela_virtual_app.data.source.firestore.LeaderboardFirestoreSource
import com.example.quiniela_virtual_app.data.source.firestore.PartidoFirestoreSource
import com.example.quiniela_virtual_app.data.source.firestore.PrediccionFirestoreSource
import com.example.quiniela_virtual_app.data.source.firestore.UsuarioFirestoreSource
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepositoryImpl @Inject constructor(
    private val leaderboardSource: LeaderboardFirestoreSource,
    private val partidoSource: PartidoFirestoreSource,
    private val prediccionSource: PrediccionFirestoreSource,
    private val usuarioSource: UsuarioFirestoreSource,
    private val configSource: ConfigFirestoreSource,
) : LeaderboardRepository {

    override fun observarLeaderboard(): Flow<List<PosicionLeaderboard>> =
        leaderboardSource.observar().map { lista -> lista.map { it.toDomain() } }

    override suspend fun recalcular(): Result<Unit> = runCatching {
        val config = configSource.obtenerSnapshot()
            ?: error("Sin configuración de competición")
        val finalizados = partidoSource.obtenerSnapshot()
            .filter { it.estado == EstadoPartido.FINALIZADO.name && !it.excluido }
        val usuarios = usuarioSource.obtenerSnapshot().filter { it.activo }

        val posiciones = usuarios.map { usuario ->
            val predicciones = prediccionSource.obtenerSnapshot(usuario.uid)
            calcularPosicion(usuario, finalizados, predicciones, config)
        }.sortedByDescending { it.puntosTotales }

        leaderboardSource.recalcular(posiciones)
    }

    private fun calcularPosicion(
        usuario: UsuarioDto,
        partidos: List<PartidoDto>,
        predicciones: List<PrediccionDto>,
        config: ConfiguracionAppDto,
    ): PosicionLeaderboardDto {
        val predMap = predicciones.associateBy { it.partidoId }
        val puntosPorJornada = mutableMapOf<String, Long>()
        var exactos = 0
        var tendencias = 0

        for (partido in partidos) {
            val pred = predMap[partido.id] ?: continue
            val puntos = calcularPuntos(partido, pred, config)
            puntosPorJornada.merge(partido.jornada.toString(), puntos.toLong(), Long::plus)
            if (puntos == config.puntosExacto.toInt()) exactos++
            else if (puntos > 0) tendencias++
        }

        return PosicionLeaderboardDto(
            uid = usuario.uid,
            nombre = usuario.nombre,
            fotoUrl = usuario.fotoUrl,
            puntosTotales = puntosPorJornada.values.sum(),
            aciertosExactos = exactos.toLong(),
            aciertosTendencia = tendencias.toLong(),
            prediccionesTotales = predMap.size.toLong(),
            puntosPorJornada = puntosPorJornada,
        )
    }

    private fun calcularPuntos(
        partido: PartidoDto,
        pred: PrediccionDto,
        config: ConfiguracionAppDto,
    ): Int {
        val local = partido.golesLocal ?: return 0
        val visitante = partido.golesVisitante ?: return 0
        if (local == pred.golesLocal && visitante == pred.golesVisitante) return config.puntosExacto.toInt()
        val tendenciaCorrecta = signo(local, visitante) == signo(pred.golesLocal, pred.golesVisitante)
        return if (tendenciaCorrecta) config.puntosTendencia.toInt() else 0
    }

    private fun signo(a: Long, b: Long): Int = when {
        a > b -> 1
        a < b -> -1
        else -> 0
    }
}
