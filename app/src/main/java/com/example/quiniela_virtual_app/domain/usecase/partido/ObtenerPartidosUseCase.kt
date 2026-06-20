package com.example.quiniela_virtual_app.domain.usecase.partido

import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerPartidosUseCase @Inject constructor(
    private val repository: PartidoRepository,
) {
    operator fun invoke(): Flow<List<Partido>> = repository.observarPartidos()
}
