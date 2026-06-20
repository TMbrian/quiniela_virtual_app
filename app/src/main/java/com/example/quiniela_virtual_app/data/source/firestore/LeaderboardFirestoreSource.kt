package com.example.quiniela_virtual_app.data.source.firestore

import com.example.quiniela_virtual_app.data.dto.LeaderboardDto
import com.example.quiniela_virtual_app.data.dto.PosicionLeaderboardDto
import com.example.quiniela_virtual_app.data.dto.toFirestoreMap
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val documento get() = firestore.collection("leaderboard").document("general")

    fun observar(): Flow<List<PosicionLeaderboardDto>> = callbackFlow {
        val listener = documento.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val posiciones = snap?.toObject(LeaderboardDto::class.java)?.posiciones ?: emptyList()
            trySend(posiciones)
        }
        awaitClose { listener.remove() }
    }

    suspend fun recalcular(posiciones: List<PosicionLeaderboardDto>): Result<Unit> = runCatching {
        val datos = mapOf(
            "posiciones" to posiciones.map { it.toFirestoreMap() },
            "actualizadoEn" to Timestamp.now(),
        )
        documento.set(datos).await()
    }
}
