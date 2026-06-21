package com.example.quiniela_virtual_app.domain.usecase.partido

import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import javax.inject.Inject

class SetLockOverrideUseCase @Inject constructor(
    private val repository: PartidoRepository,
) {
    suspend operator fun invoke(partidoId: String, unlock: Boolean, expiryMs: Long?): Result<Unit> =
        repository.setLockOverride(partidoId, unlock, expiryMs)
}
