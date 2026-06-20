package com.example.quiniela_virtual_app.domain.usecase.partido

import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import javax.inject.Inject

class SincronizarPartidosUseCase @Inject constructor(
    private val repository: PartidoRepository,
) {
    suspend operator fun invoke(): Result<Int> = repository.sincronizarDesdeApi()
}
