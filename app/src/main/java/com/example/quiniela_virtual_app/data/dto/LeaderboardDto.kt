package com.example.quiniela_virtual_app.data.dto

import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.google.firebase.Timestamp

data class LeaderboardDto(
    val actualizadoEn: Timestamp = Timestamp.now(),
    val posiciones: List<PosicionLeaderboardDto> = emptyList(),
)

data class PosicionLeaderboardDto(
    val uid: String = "",
    val nombre: String = "",
    val fotoUrl: String? = null,
    val puntosTotales: Long = 0,
    val aciertosExactos: Long = 0,
    val aciertosTendencia: Long = 0,
    val prediccionesTotales: Long = 0,
    val puntosPorJornada: Map<String, Long> = emptyMap(),
)

fun PosicionLeaderboardDto.toDomain(): PosicionLeaderboard = PosicionLeaderboard(
    uid = uid,
    nombre = nombre,
    fotoUrl = fotoUrl,
    puntosTotales = puntosTotales.toInt(),
    aciertosExactos = aciertosExactos.toInt(),
    aciertosTendencia = aciertosTendencia.toInt(),
    prediccionesTotales = prediccionesTotales.toInt(),
    puntosPorJornada = puntosPorJornada.mapValues { it.value.toInt() },
)

fun PosicionLeaderboardDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "nombre" to nombre,
    "fotoUrl" to fotoUrl,
    "puntosTotales" to puntosTotales,
    "aciertosExactos" to aciertosExactos,
    "aciertosTendencia" to aciertosTendencia,
    "prediccionesTotales" to prediccionesTotales,
    "puntosPorJornada" to puntosPorJornada,
)
