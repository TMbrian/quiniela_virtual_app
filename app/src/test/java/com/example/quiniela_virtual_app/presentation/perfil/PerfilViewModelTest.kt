package com.example.quiniela_virtual_app.presentation.perfil

import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.domain.usecase.usuario.ObtenerUsuarioUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class PerfilViewModelTest {

    private val obtenerUsuario = mockk<ObtenerUsuarioUseCase>()
    private val leaderboardRepo = mockk<LeaderboardRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun crearViewModel() = PerfilViewModel(obtenerUsuario, leaderboardRepo)

    private fun usuarioFake(uid: String = "u1") = Usuario(
        uid = uid, nombre = "Prueba", email = "a@b.com",
        fotoUrl = null, esAdmin = false, activo = true,
        creadoEn = Instant.EPOCH, ultimoAcceso = Instant.EPOCH,
    )

    private fun posicionFake(uid: String = "u1", puntos: Int = 10) = PosicionLeaderboard(
        uid = uid, nombre = "Prueba", fotoUrl = null,
        puntosTotales = puntos, aciertosExactos = 2, aciertosTendencia = 1,
        prediccionesTotales = 5, puntosPorJornada = mapOf("1" to puntos),
    )

    @Test
    fun `usuario null retorna error`() = runTest {
        every { obtenerUsuario() } returns flowOf(null)
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())

        assertTrue(crearViewModel().uiState.value is UiState.Error)
    }

    @Test
    fun `usuario en primera posicion del leaderboard`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake("u1"))
        every { leaderboardRepo.observarLeaderboard() } returns
            flowOf(listOf(posicionFake("u1", 20), posicionFake("u2", 10)))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(1, data.puesto)
        assertEquals(20, data.stats?.puntosTotales)
        assertEquals("u1", data.usuario.uid)
    }

    @Test
    fun `usuario en segunda posicion del leaderboard`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake("u2"))
        every { leaderboardRepo.observarLeaderboard() } returns
            flowOf(listOf(posicionFake("u1", 20), posicionFake("u2", 10)))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(2, data.puesto)
        assertEquals(10, data.stats?.puntosTotales)
    }

    @Test
    fun `usuario no aparece en leaderboard retorna puesto null y stats null`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake("u99"))
        every { leaderboardRepo.observarLeaderboard() } returns
            flowOf(listOf(posicionFake("u1", 20), posicionFake("u2", 10)))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertNull(data.puesto)
        assertNull(data.stats)
    }

    @Test
    fun `leaderboard vacio retorna puesto null y stats null`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake("u1"))
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertNull(data.puesto)
        assertNull(data.stats)
        assertEquals("u1", data.usuario.uid)
    }
}
