package com.example.quiniela_virtual_app.domain.usecase.partido

import com.example.quiniela_virtual_app.domain.repository.ConfigRepository
import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SincronizarPartidosUseCase @Inject constructor(
    private val repository: PartidoRepository,
    private val configRepository: ConfigRepository,
) {
    suspend operator fun invoke(): Result<Int> {
        val config = configRepository.observarConfig().first()
            ?: return Result.failure(IllegalStateException("No hay configuración. Guarda la configuración de la quiniela primero."))
        return repository.sincronizarDesdeApi(config.codigoApi)
    }
}
