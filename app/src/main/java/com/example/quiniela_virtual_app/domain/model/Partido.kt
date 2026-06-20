package com.example.quiniela_virtual_app.domain.model

import java.time.Instant

data class Equipo(
    val nombre: String,
    val iso: String,
)

data class Partido(
    val id: String,
    val jornada: Int,
    val grupo: String,
    val fase: String,
    val equipoLocal: Equipo,
    val equipoVisitante: Equipo,
    val fecha: Instant,
    val sede: String,
    val golesLocal: Int?,
    val golesVisitante: Int?,
    val estado: EstadoPartido,
    val excluido: Boolean = false,
    val lockOverride: Boolean? = null,
    val lockOverrideExpiry: Instant? = null,
    val apiId: String? = null,
)
