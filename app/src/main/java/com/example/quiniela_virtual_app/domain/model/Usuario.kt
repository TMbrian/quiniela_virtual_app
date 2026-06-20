package com.example.quiniela_virtual_app.domain.model

import java.time.Instant

data class Usuario(
    val uid: String,
    val nombre: String,
    val email: String,
    val fotoUrl: String?,
    val esAdmin: Boolean,
    val activo: Boolean,
    val creadoEn: Instant,
    val ultimoAcceso: Instant,
)
