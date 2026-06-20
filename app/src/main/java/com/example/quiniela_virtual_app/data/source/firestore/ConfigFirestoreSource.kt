package com.example.quiniela_virtual_app.data.source.firestore

import com.example.quiniela_virtual_app.data.dto.ConfiguracionAppDto
import com.example.quiniela_virtual_app.data.dto.toFirestoreMap
import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val documento get() = firestore.collection("config").document("app")

    fun observar(): Flow<ConfiguracionAppDto?> = callbackFlow {
        val listener = documento.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(ConfiguracionAppDto::class.java))
        }
        awaitClose { listener.remove() }
    }

    suspend fun obtenerSnapshot(): ConfiguracionAppDto? =
        documento.get().await().toObject(ConfiguracionAppDto::class.java)

    suspend fun guardar(config: ConfiguracionApp): Result<Unit> = runCatching {
        documento.set(config.toFirestoreMap()).await()
    }
}
