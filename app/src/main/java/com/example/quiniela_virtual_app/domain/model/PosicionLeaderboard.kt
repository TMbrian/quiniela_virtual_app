package com.example.quiniela_virtual_app.domain.model

data class PosicionLeaderboard(
    val uid: String,
    val nombre: String,
    val fotoUrl: String?,
    val puntosTotales: Int,
    val aciertosExactos: Int,
    val aciertosTendencia: Int,
    val prediccionesTotales: Int,
    val puntosPorJornada: Map<String, Int>,
)
