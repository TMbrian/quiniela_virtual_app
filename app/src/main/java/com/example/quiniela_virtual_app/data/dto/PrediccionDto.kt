package com.example.quiniela_virtual_app.data.dto

import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.google.firebase.Timestamp
import java.time.Instant

data class PrediccionDto(
    val partidoId: String = "",
    val uid: String = "",
    val golesLocal: Long = 0,
    val golesVisitante: Long = 0,
    val creadaEn: Timestamp = Timestamp.now(),
    val actualizadaEn: Timestamp = Timestamp.now(),
)

fun PrediccionDto.toDomain(): Prediccion = Prediccion(
    uid = uid,
    partidoId = partidoId,
    golesLocal = golesLocal.toInt(),
    golesVisitante = golesVisitante.toInt(),
    creadaEn = Instant.ofEpochSecond(creadaEn.seconds, creadaEn.nanoseconds.toLong()),
    actualizadaEn = Instant.ofEpochSecond(actualizadaEn.seconds, actualizadaEn.nanoseconds.toLong()),
)

fun Prediccion.toFirestoreMap(): Map<String, Any> = mapOf(
    "uid" to uid,
    "partidoId" to partidoId,
    "golesLocal" to golesLocal.toLong(),
    "golesVisitante" to golesVisitante.toLong(),
    "creadaEn" to Timestamp(creadaEn.epochSecond, creadaEn.nano),
    "actualizadaEn" to Timestamp(actualizadaEn.epochSecond, actualizadaEn.nano),
)
