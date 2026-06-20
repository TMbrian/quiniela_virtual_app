package com.example.quiniela_virtual_app.domain.repository

import com.example.quiniela_virtual_app.domain.model.Prediccion
import kotlinx.coroutines.flow.Flow

interface PrediccionRepository {
    fun observarPredicciones(uid: String): Flow<List<Prediccion>>
    suspend fun guardarPrediccion(prediccion: Prediccion): Result<Unit>
}
