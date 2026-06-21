package com.example.quiniela_virtual_app.data.dto

import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import com.google.firebase.Timestamp
import java.time.Instant

data class ConfiguracionAppDto(
    val nombreCompeticion: String = "",
    val descripcion: String = "",
    val temporada: String = "",
    val codigoApi: String = "WC",
    val puntosExacto: Long = 5,
    val puntosTendencia: Long = 3,
    val lockAnticipacionMin: Long = 30,
    val activa: Boolean = true,
    val creadaEn: Timestamp = Timestamp.now(),
    val actualizadaEn: Timestamp = Timestamp.now(),
)

fun ConfiguracionAppDto.toDomain(): ConfiguracionApp = ConfiguracionApp(
    nombreCompeticion = nombreCompeticion,
    descripcion = descripcion,
    temporada = temporada,
    codigoApi = codigoApi,
    puntosExacto = puntosExacto.toInt(),
    puntosTendencia = puntosTendencia.toInt(),
    lockAnticipacionMin = lockAnticipacionMin.toInt(),
    activa = activa,
    creadaEn = Instant.ofEpochSecond(creadaEn.seconds, creadaEn.nanoseconds.toLong()),
    actualizadaEn = Instant.ofEpochSecond(actualizadaEn.seconds, actualizadaEn.nanoseconds.toLong()),
)

fun ConfiguracionApp.toFirestoreMap(): Map<String, Any> = mapOf(
    "nombreCompeticion" to nombreCompeticion,
    "descripcion" to descripcion,
    "temporada" to temporada,
    "codigoApi" to codigoApi,
    "puntosExacto" to puntosExacto.toLong(),
    "puntosTendencia" to puntosTendencia.toLong(),
    "lockAnticipacionMin" to lockAnticipacionMin.toLong(),
    "activa" to activa,
    "creadaEn" to Timestamp(creadaEn.epochSecond, creadaEn.nano),
    "actualizadaEn" to Timestamp(actualizadaEn.epochSecond, actualizadaEn.nano),
)
