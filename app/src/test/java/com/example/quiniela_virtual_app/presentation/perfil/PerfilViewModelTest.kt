package com.example.quiniela_virtual_app.presentation.perfil

import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.domain.model.Usuario
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.domain.usecase.partido.ObtenerPartidosUseCase
import com.example.quiniela_virtual_app.domain.usecase.prediccion.ObtenerPrediccionesUseCase
import com.example.quiniela_virtual_app.domain.usecase.usuario.ObtenerUsuarioUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
    private val obtenerPartidos = mockk<ObtenerPartidosUseCase>()
    private val obtenerPredicciones = mockk<ObtenerPrediccionesUseCase>()
    private val auth = mockk<FirebaseAuth>()
    private val mockUser = mockk<FirebaseUser>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "u1"
        every { obtenerPartidos() } returns flowOf(emptyList())
        every { obtenerPredicciones("u1") } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun crearViewModel() = PerfilViewModel(
        obtenerUsuario, leaderboardRepo, obtenerPartidos, obtenerPredicciones, auth,
    )

    // ── Helpers ──────────────────────────────────────────────────────────────────

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

    private fun partidoFake(
        id: String = "p1",
        estado: EstadoPartido = EstadoPartido.FINALIZADO,
        golesLocal: Int? = 1,
        golesVisitante: Int? = 0,
        excluido: Boolean = false,
        fechaEpochMs: Long = 1_000L,
    ) = Partido(
        id = id, jornada = 1, grupo = "A", fase = "Grupos",
        equipoLocal = Equipo("Local", "LOC"),
        equipoVisitante = Equipo("Visitante", "VIS"),
        fecha = Instant.ofEpochMilli(fechaEpochMs),
        sede = "Estadio",
        golesLocal = golesLocal, golesVisitante = golesVisitante,
        estado = estado, excluido = excluido,
    )

    private fun prediccionFake(
        partidoId: String = "p1",
        gL: Int = 1,
        gV: Int = 0,
    ) = Prediccion(
        uid = "u1", partidoId = partidoId,
        golesLocal = gL, golesVisitante = gV,
        creadaEn = Instant.EPOCH, actualizadaEn = Instant.EPOCH,
    )

    // ── construirPerfilData ───────────────────────────────────────────────────────

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

    // ── calcularRacha ─────────────────────────────────────────────────────────────

    @Test
    fun `racha cero cuando no hay partidos finalizados`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        every { obtenerPartidos() } returns flowOf(listOf(
            partidoFake(estado = EstadoPartido.PROGRAMADO, golesLocal = null, golesVisitante = null),
        ))
        every { obtenerPredicciones("u1") } returns flowOf(listOf(prediccionFake()))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(0, data.rachaActual)
    }

    @Test
    fun `racha cero cuando no hay prediccion para el partido`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        every { obtenerPartidos() } returns flowOf(listOf(partidoFake()))
        every { obtenerPredicciones("u1") } returns flowOf(emptyList())

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(0, data.rachaActual)
    }

    @Test
    fun `racha de uno con acierto exacto`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        every { obtenerPartidos() } returns flowOf(listOf(
            partidoFake("p1", golesLocal = 2, golesVisitante = 1),
        ))
        every { obtenerPredicciones("u1") } returns flowOf(listOf(
            prediccionFake("p1", gL = 2, gV = 1),
        ))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(1, data.rachaActual)
    }

    @Test
    fun `tendencia correcta cuenta como acierto en la racha`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        every { obtenerPartidos() } returns flowOf(listOf(
            partidoFake("p1", golesLocal = 3, golesVisitante = 0),
        ))
        every { obtenerPredicciones("u1") } returns flowOf(listOf(
            prediccionFake("p1", gL = 1, gV = 0),  // local gana en ambos
        ))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(1, data.rachaActual)
    }

    @Test
    fun `racha de dos con dos aciertos consecutivos`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        // p2 es más reciente (fechaEpochMs mayor) → se itera primero en orden desc
        every { obtenerPartidos() } returns flowOf(listOf(
            partidoFake("p1", golesLocal = 1, golesVisitante = 0, fechaEpochMs = 1_000L),
            partidoFake("p2", golesLocal = 2, golesVisitante = 1, fechaEpochMs = 2_000L),
        ))
        every { obtenerPredicciones("u1") } returns flowOf(listOf(
            prediccionFake("p1", gL = 1, gV = 0),  // local gana: correcto
            prediccionFake("p2", gL = 1, gV = 0),  // local gana: correcto
        ))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(2, data.rachaActual)
    }

    @Test
    fun `racha se corta en el primer fallo`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        // orden desc: p2 (reciente) → p1 (antiguo)
        every { obtenerPartidos() } returns flowOf(listOf(
            partidoFake("p1", golesLocal = 1, golesVisitante = 0, fechaEpochMs = 1_000L),
            partidoFake("p2", golesLocal = 1, golesVisitante = 0, fechaEpochMs = 2_000L),
        ))
        every { obtenerPredicciones("u1") } returns flowOf(listOf(
            prediccionFake("p1", gL = 0, gV = 1),  // fallo en p1 (más antiguo)
            prediccionFake("p2", gL = 1, gV = 0),  // acierto en p2 (más reciente)
        ))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        // p2 ✓ racha=1 → p1 ✗ break → racha final = 1
        assertEquals(1, data.rachaActual)
    }

    @Test
    fun `partido excluido se ignora y no interrumpe la racha`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        // orden desc: p3 → p2 (excluido, filtrado) → p1
        every { obtenerPartidos() } returns flowOf(listOf(
            partidoFake("p1", golesLocal = 1, golesVisitante = 0, fechaEpochMs = 1_000L),
            partidoFake("p2", golesLocal = 0, golesVisitante = 1, excluido = true, fechaEpochMs = 2_000L),
            partidoFake("p3", golesLocal = 1, golesVisitante = 0, fechaEpochMs = 3_000L),
        ))
        every { obtenerPredicciones("u1") } returns flowOf(listOf(
            prediccionFake("p1", gL = 1, gV = 0),  // acierto
            prediccionFake("p3", gL = 1, gV = 0),  // acierto
        ))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        // p2 excluido → ignorado; p3 ✓ p1 ✓ → racha = 2
        assertEquals(2, data.rachaActual)
    }

    @Test
    fun `partido sin goles cargados corta la racha`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        every { obtenerPartidos() } returns flowOf(listOf(
            partidoFake("p1", golesLocal = null, golesVisitante = null),
        ))
        every { obtenerPredicciones("u1") } returns flowOf(listOf(prediccionFake("p1")))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(0, data.rachaActual)
    }

    @Test
    fun `partidos no finalizados no contribuyen a la racha`() = runTest {
        every { obtenerUsuario() } returns flowOf(usuarioFake())
        every { leaderboardRepo.observarLeaderboard() } returns flowOf(emptyList())
        every { obtenerPartidos() } returns flowOf(listOf(
            partidoFake("p1", estado = EstadoPartido.PROGRAMADO, golesLocal = null, golesVisitante = null),
            partidoFake("p2", estado = EstadoPartido.EN_VIVO,    golesLocal = 1,    golesVisitante = 0, fechaEpochMs = 2_000L),
        ))
        every { obtenerPredicciones("u1") } returns flowOf(listOf(
            prediccionFake("p1"),
            prediccionFake("p2"),
        ))

        val data = (crearViewModel().uiState.value as UiState.Success).data

        assertEquals(0, data.rachaActual)
    }
}
