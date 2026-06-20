package com.example.quiniela_virtual_app.data.repository

import com.example.quiniela_virtual_app.data.dto.toDomain
import com.example.quiniela_virtual_app.data.source.firestore.UsuarioFirestoreSource
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioRepositoryImpl @Inject constructor(
    private val source: UsuarioFirestoreSource,
) : UsuarioRepository {

    override fun observarUsuarioActual(): Flow<Usuario?> =
        source.observarActual().map { it?.toDomain() }

    override fun observarUsuarios(): Flow<List<Usuario>> =
        source.observarTodos().map { lista -> lista.map { it.toDomain() } }

    override suspend fun crearPerfil(usuario: Usuario): Result<Unit> =
        source.crear(usuario)

    override suspend fun actualizarNombre(uid: String, nombre: String): Result<Unit> =
        source.actualizarNombre(uid, nombre)

    override suspend fun actualizarFotoUrl(uid: String, fotoUrl: String): Result<Unit> =
        source.actualizarFotoUrl(uid, fotoUrl)

    override suspend fun cambiarRol(uid: String, esAdmin: Boolean): Result<Unit> =
        source.cambiarRol(uid, esAdmin)

    override suspend fun cambiarEstadoActivo(uid: String, activo: Boolean): Result<Unit> =
        source.cambiarEstadoActivo(uid, activo)
}
