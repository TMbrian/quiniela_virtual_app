package com.example.quiniela_virtual_app.data.dto

import com.example.quiniela_virtual_app.domain.model.Usuario
import com.google.firebase.Timestamp
import java.time.Instant

data class UsuarioDto(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val fotoUrl: String? = null,
    val esAdmin: Boolean = false,
    val activo: Boolean = true,
    val creadoEn: Timestamp = Timestamp.now(),
    val ultimoAcceso: Timestamp = Timestamp.now(),
)

fun UsuarioDto.toDomain(): Usuario = Usuario(
    uid = uid,
    nombre = nombre,
    email = email,
    fotoUrl = fotoUrl,
    esAdmin = esAdmin,
    activo = activo,
    creadoEn = Instant.ofEpochSecond(creadoEn.seconds, creadoEn.nanoseconds.toLong()),
    ultimoAcceso = Instant.ofEpochSecond(ultimoAcceso.seconds, ultimoAcceso.nanoseconds.toLong()),
)

fun Usuario.toFirestoreMap(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "nombre" to nombre,
    "email" to email,
    "fotoUrl" to fotoUrl,
    "esAdmin" to esAdmin,
    "activo" to activo,
    "creadoEn" to Timestamp(creadoEn.epochSecond, creadoEn.nano),
    "ultimoAcceso" to Timestamp(ultimoAcceso.epochSecond, ultimoAcceso.nano),
)
