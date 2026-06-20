package com.example.quiniela_virtual_app.domain.usecase.usuario

import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerUsuarioUseCase @Inject constructor(
    private val repository: UsuarioRepository,
) {
    operator fun invoke(): Flow<Usuario?> = repository.observarUsuarioActual()
}
