package com.example.quiniela_virtual_app.data.dto

import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.google.firebase.Timestamp
import java.time.Instant

data class EquipoDto(
    val nombre: String = "",
    val iso: String = "",
)

data class PartidoDto(
    val id: String = "",
    val jornada: Int = 0,
    val grupo: String = "",
    val fase: String = "",
    val equipoLocal: EquipoDto = EquipoDto(),
    val equipoVisitante: EquipoDto = EquipoDto(),
    val fecha: Timestamp = Timestamp.now(),
    val sede: String = "",
    val golesLocal: Long? = null,
    val golesVisitante: Long? = null,
    val estado: String = EstadoPartido.PROGRAMADO.name,
    val excluido: Boolean = false,
    val lockOverride: Boolean? = null,
    val lockOverrideExpiry: Timestamp? = null,
    val apiId: String? = null,
)

fun PartidoDto.toDomain(): Partido = Partido(
    id = id,
    jornada = jornada,
    grupo = grupo,
    fase = fase,
    equipoLocal = Equipo(equipoLocal.nombre, equipoLocal.iso),
    equipoVisitante = Equipo(equipoVisitante.nombre, equipoVisitante.iso),
    fecha = Instant.ofEpochSecond(fecha.seconds, fecha.nanoseconds.toLong()),
    sede = sede,
    golesLocal = golesLocal?.toInt(),
    golesVisitante = golesVisitante?.toInt(),
    estado = runCatching { EstadoPartido.valueOf(estado) }.getOrDefault(EstadoPartido.PROGRAMADO),
    excluido = excluido,
    lockOverride = lockOverride,
    lockOverrideExpiry = lockOverrideExpiry?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) },
    apiId = apiId,
)

fun Partido.toFirestoreMap(): Map<String, Any?> = mapOf(
    "jornada" to jornada,
    "grupo" to grupo,
    "fase" to fase,
    "equipoLocal" to mapOf("nombre" to equipoLocal.nombre, "iso" to equipoLocal.iso),
    "equipoVisitante" to mapOf("nombre" to equipoVisitante.nombre, "iso" to equipoVisitante.iso),
    "fecha" to Timestamp(fecha.epochSecond, fecha.nano),
    "sede" to sede,
    "golesLocal" to golesLocal,
    "golesVisitante" to golesVisitante,
    "estado" to estado.name,
    "excluido" to excluido,
    "lockOverride" to lockOverride,
    "lockOverrideExpiry" to lockOverrideExpiry?.let { Timestamp(it.epochSecond, it.nano) },
    "apiId" to apiId,
)
