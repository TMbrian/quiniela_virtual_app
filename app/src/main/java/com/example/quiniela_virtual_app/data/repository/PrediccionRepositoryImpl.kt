package com.example.quiniela_virtual_app.data.repository

import com.example.quiniela_virtual_app.data.dto.toDomain
import com.example.quiniela_virtual_app.data.source.firestore.PrediccionFirestoreSource
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.domain.repository.PrediccionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrediccionRepositoryImpl @Inject constructor(
    private val source: PrediccionFirestoreSource,
) : PrediccionRepository {

    override fun observarPredicciones(uid: String): Flow<List<Prediccion>> =
        source.observar(uid).map { lista -> lista.map { it.toDomain() } }

    override suspend fun guardarPrediccion(prediccion: Prediccion): Result<Unit> =
        source.guardar(prediccion.uid, prediccion)
}
