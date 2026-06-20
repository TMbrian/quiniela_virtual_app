package com.example.quiniela_virtual_app.data.source.firestore

import com.example.quiniela_virtual_app.data.dto.UsuarioDto
import com.example.quiniela_virtual_app.data.dto.toFirestoreMap
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    private val coleccion get() = firestore.collection("usuarios")

    fun observarActual(): Flow<UsuarioDto?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = coleccion.document(uid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(UsuarioDto::class.java)?.copy(uid = snap.id))
        }
        awaitClose { listener.remove() }
    }

    fun observarTodos(): Flow<List<UsuarioDto>> = callbackFlow {
        val listener = coleccion.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val lista = snap?.documents?.mapNotNull { doc ->
                doc.toObject(UsuarioDto::class.java)?.copy(uid = doc.id)
            } ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    suspend fun obtenerSnapshot(): List<UsuarioDto> =
        coleccion.get().await().documents.mapNotNull { doc ->
            doc.toObject(UsuarioDto::class.java)?.copy(uid = doc.id)
        }

    suspend fun crear(usuario: Usuario): Result<Unit> = runCatching {
        coleccion.document(usuario.uid).set(usuario.toFirestoreMap()).await()
    }

    suspend fun actualizarNombre(uid: String, nombre: String): Result<Unit> = runCatching {
        coleccion.document(uid).update("nombre", nombre).await()
    }

    suspend fun actualizarFotoUrl(uid: String, fotoUrl: String): Result<Unit> = runCatching {
        coleccion.document(uid).update("fotoUrl", fotoUrl).await()
    }

    suspend fun cambiarRol(uid: String, esAdmin: Boolean): Result<Unit> = runCatching {
        coleccion.document(uid).update("esAdmin", esAdmin).await()
    }

    suspend fun cambiarEstadoActivo(uid: String, activo: Boolean): Result<Unit> = runCatching {
        coleccion.document(uid).update("activo", activo).await()
    }
}
