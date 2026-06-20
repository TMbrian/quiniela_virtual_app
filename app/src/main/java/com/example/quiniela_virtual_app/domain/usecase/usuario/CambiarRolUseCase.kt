package com.example.quiniela_virtual_app.domain.usecase.usuario

import com.example.quiniela_virtual_app.domain.repository.UsuarioRepository
import javax.inject.Inject

class CambiarRolUseCase @Inject constructor(
    private val repository: UsuarioRepository,
) {
    suspend operator fun invoke(uid: String, esAdmin: Boolean): Result<Unit> =
        repository.cambiarRol(uid, esAdmin)
}
