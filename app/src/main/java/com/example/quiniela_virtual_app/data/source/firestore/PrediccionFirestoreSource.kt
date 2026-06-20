package com.example.quiniela_virtual_app.data.source.firestore

import com.example.quiniela_virtual_app.data.dto.PrediccionDto
import com.example.quiniela_virtual_app.data.dto.toFirestoreMap
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrediccionFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun coleccion(uid: String) =
        firestore.collection("predicciones").document(uid).collection("partidos")

    fun observar(uid: String): Flow<List<PrediccionDto>> = callbackFlow {
        val listener = coleccion(uid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val lista = snap?.documents?.mapNotNull { doc ->
                doc.toObject(PrediccionDto::class.java)?.copy(partidoId = doc.id)
            } ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    suspend fun obtenerSnapshot(uid: String): List<PrediccionDto> =
        coleccion(uid).get().await().documents.mapNotNull { doc ->
            doc.toObject(PrediccionDto::class.java)?.copy(partidoId = doc.id)
        }

    suspend fun guardar(uid: String, prediccion: Prediccion): Result<Unit> = runCatching {
        coleccion(uid).document(prediccion.partidoId).set(prediccion.toFirestoreMap()).await()
    }
}
