package com.example.quiniela_virtual_app.domain.usecase.partido

import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import javax.inject.Inject

class CambiarEstadoUseCase @Inject constructor(
    private val repository: PartidoRepository,
) {
    suspend operator fun invoke(partidoId: String, estado: EstadoPartido): Result<Unit> =
        repository.cambiarEstado(partidoId, estado)
}
