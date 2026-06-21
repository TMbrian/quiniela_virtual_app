package com.example.quiniela_virtual_app.domain.usecase.partido

import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import javax.inject.Inject

class SetExcluidoUseCase @Inject constructor(
    private val repository: PartidoRepository,
) {
    suspend operator fun invoke(partidoId: String, excluido: Boolean): Result<Unit> =
        repository.setExcluido(partidoId, excluido)
}
