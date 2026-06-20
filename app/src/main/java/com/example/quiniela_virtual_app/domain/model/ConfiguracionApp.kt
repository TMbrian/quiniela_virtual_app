package com.example.quiniela_virtual_app.domain.model

import java.time.Instant

data class ConfiguracionApp(
    val nombreCompeticion: String,
    val descripcion: String,
    val temporada: String,
    val puntosExacto: Int,
    val puntosTendencia: Int,
    val lockAnticipacionMin: Int,
    val activa: Boolean,
    val creadaEn: Instant,
    val actualizadaEn: Instant,
)
