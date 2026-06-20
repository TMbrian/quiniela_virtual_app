package com.example.quiniela_virtual_app.domain.repository

import com.example.quiniela_virtual_app.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun observarUsuarioActual(): Flow<Usuario?>
    fun observarUsuarios(): Flow<List<Usuario>>
    suspend fun crearPerfil(usuario: Usuario): Result<Unit>
    suspend fun actualizarNombre(uid: String, nombre: String): Result<Unit>
    suspend fun actualizarFotoUrl(uid: String, fotoUrl: String): Result<Unit>
    suspend fun cambiarRol(uid: String, esAdmin: Boolean): Result<Unit>
    suspend fun cambiarEstadoActivo(uid: String, activo: Boolean): Result<Unit>
}
