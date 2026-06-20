package com.example.quiniela_virtual_app.domain.usecase.prediccion

import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.domain.repository.PrediccionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerPrediccionesUseCase @Inject constructor(
    private val repository: PrediccionRepository,
) {
    operator fun invoke(uid: String): Flow<List<Prediccion>> =
        repository.observarPredicciones(uid)
}
