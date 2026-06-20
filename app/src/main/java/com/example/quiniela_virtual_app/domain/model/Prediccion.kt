package com.example.quiniela_virtual_app.domain.model

import java.time.Instant

data class Prediccion(
    val uid: String,
    val partidoId: String,
    val golesLocal: Int,
    val golesVisitante: Int,
    val creadaEn: Instant,
    val actualizadaEn: Instant,
)
