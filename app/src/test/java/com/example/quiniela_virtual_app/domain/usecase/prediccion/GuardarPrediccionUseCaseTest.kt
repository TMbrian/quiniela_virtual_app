package com.example.quiniela_virtual_app.domain.usecase.prediccion

import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion.ABIERTO
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion.BLOQUEADO
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.domain.repository.PrediccionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class GuardarPrediccionUseCaseTest {

    private val repository = mockk<PrediccionRepository>()
    private val calcularEstado = mockk<CalcularEstadoPrediccionUseCase>()
    private val useCase = GuardarPrediccionUseCase(repository, calcularEstado)

    private val LOCK_MS = 30 * 60_000L

    private fun partidoFake() = Partido(
        id = "p1", jornada = 1, grupo = "A", fase = "Grupos",
        equipoLocal = Equipo("Local", "LOC"),
        equipoVisitante = Equipo("Visitante", "VIS"),
        fecha = Instant.now(),
        sede = "Estadio",
        golesLocal = null, golesVisitante = null,
        estado = EstadoPartido.PROGRAMADO,
    )

    private fun prediccionFake() = Prediccion(
        uid = "u1", partidoId = "p1",
        golesLocal = 2, golesVisitante = 1,
        creadaEn = Instant.now(), actualizadaEn = Instant.now(),
    )

    @Test
    fun `partido bloqueado retorna failure sin llamar al repositorio`() = runTest {
        every { calcularEstado(any(), any(), any()) } returns BLOQUEADO

        val resultado = useCase(prediccionFake(), partidoFake(), LOCK_MS)

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { repository.guardarPrediccion(any()) }
    }

    @Test
    fun `partido abierto y repositorio ok retorna success`() = runTest {
        every { calcularEstado(any(), any(), any()) } returns ABIERTO
        coEvery { repository.guardarPrediccion(any()) } returns Result.success(Unit)

        val resultado = useCase(prediccionFake(), partidoFake(), LOCK_MS)

        assertTrue(resultado.isSuccess)
    }

    @Test
    fun `partido abierto y repositorio falla propaga el failure`() = runTest {
        val error = RuntimeException("Error de Firestore")
        every { calcularEstado(any(), any(), any()) } returns ABIERTO
        coEvery { repository.guardarPrediccion(any()) } returns Result.failure(error)

        val resultado = useCase(prediccionFake(), partidoFake(), LOCK_MS)

        assertTrue(resultado.isFailure)
        assertEquals(error, resultado.exceptionOrNull())
    }
}
