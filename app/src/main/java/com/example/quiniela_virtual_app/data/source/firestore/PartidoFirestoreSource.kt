package com.example.quiniela_virtual_app.data.source.firestore

import com.example.quiniela_virtual_app.data.dto.PartidoDto
import com.example.quiniela_virtual_app.data.dto.toFirestoreMap
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartidoFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val coleccion get() = firestore.collection("partidos")

    fun observar(): Flow<List<PartidoDto>> = callbackFlow {
        val listener = coleccion.orderBy("fecha").addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val lista = snap?.documents?.mapNotNull { doc ->
                doc.toObject(PartidoDto::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    suspend fun obtenerSnapshot(): List<PartidoDto> =
        coleccion.get().await().documents.mapNotNull { doc ->
            doc.toObject(PartidoDto::class.java)?.copy(id = doc.id)
        }

    suspend fun actualizarResultado(id: String, golesLocal: Int, golesVisitante: Int): Result<Unit> =
        runCatching {
            coleccion.document(id).update(
                "golesLocal", golesLocal.toLong(),
                "golesVisitante", golesVisitante.toLong(),
            ).await()
        }

    suspend fun cambiarEstado(id: String, estado: EstadoPartido): Result<Unit> =
        runCatching {
            coleccion.document(id).update("estado", estado.name).await()
        }

    suspend fun setLockOverride(id: String, unlock: Boolean, expiryMs: Long?): Result<Unit> =
        runCatching {
            val updates = hashMapOf<String, Any>("lockOverride" to !unlock)
            if (unlock && expiryMs != null) {
                updates["lockOverrideExpiry"] = Timestamp(expiryMs / 1000, ((expiryMs % 1000) * 1_000_000).toInt())
            } else {
                updates["lockOverrideExpiry"] = FieldValue.delete()
            }
            coleccion.document(id).update(updates).await()
        }

    suspend fun setExcluido(id: String, excluido: Boolean): Result<Unit> =
        runCatching {
            coleccion.document(id).update("excluido", excluido).await()
        }

    /** Escribe en batch todos los partidos sincronizados desde la API (upsert por ID). */
    suspend fun sincronizar(partidos: List<Partido>): Result<Unit> = runCatching {
        val batch = firestore.batch()
        partidos.forEach { partido ->
            batch.set(coleccion.document(partido.id), partido.toFirestoreMap())
        }
        batch.commit().await()
    }

    suspend fun restablecerTodos(): Result<Unit> = runCatching {
        val docs = coleccion.get().await().documents
        val batch = firestore.batch()
        val actualizaciones = hashMapOf<String, Any>(
            "estado" to EstadoPartido.PROGRAMADO.name,
            "golesLocal" to FieldValue.delete(),
            "golesVisitante" to FieldValue.delete(),
            "lockOverride" to FieldValue.delete(),
            "lockOverrideExpiry" to FieldValue.delete(),
        )
        docs.forEach { doc -> batch.update(doc.reference, actualizaciones) }
        batch.commit().await()
    }
}
