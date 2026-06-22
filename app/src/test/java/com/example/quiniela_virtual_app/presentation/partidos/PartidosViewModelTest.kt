package com.example.quiniela_virtual_app.presentation.partidos

import com.example.quiniela_virtual_app.domain.model.ConfiguracionApp
import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
import com.example.quiniela_virtual_app.domain.repository.ConfigRepository
import com.example.quiniela_virtual_app.domain.usecase.prediccion.CalcularEstadoPrediccionUseCase
import com.example.quiniela_virtual_app.domain.usecase.prediccion.GuardarPrediccionUseCase
import com.example.quiniela_virtual_app.domain.usecase.prediccion.ObtenerPrediccionesUseCase
import com.example.quiniela_virtual_app.domain.usecase.partido.ObtenerPartidosUseCase
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

class PartidosViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun crearViewModel(
        partidos: List<Partido> = emptyList(),
        predicciones: List<Prediccion> = emptyList(),
        config: ConfiguracionApp? = configFake(),
        uid: String = "u1",
    ): PartidosViewModel {
        val mockUser = mockk<FirebaseUser>()
        val mockAuth = mockk<FirebaseAuth>()
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns uid

        return PartidosViewModel(
            obtenerPartidos = mockk<ObtenerPartidosUseCase>().also {
                every { it() } returns flowOf(partidos)
            },
            obtenerPredicciones = mockk<ObtenerPrediccionesUseCase>().also {
                every { it(uid) } returns flowOf(predicciones)
            },
            guardarPrediccionUC = mockk<GuardarPrediccionUseCase>(),
            calcularEstado = CalcularEstadoPrediccionUseCase(),
            configRepository = mockk<ConfigRepository>().also {
                every { it.observarConfig() } returns flowOf(config)
            },
            auth = mockAuth,
        )
    }

    private fun partidoFake(
        id: String = "p1",
        jornada: Int = 1,
        grupo: String = "A",
        fase: String = "Grupos",
        estado: EstadoPartido = EstadoPartido.PROGRAMADO,
        golesLocal: Int? = null,
        golesVisitante: Int? = null,
        excluido: Boolean = false,
    ) = Partido(
        id = id, jornada = jornada, grupo = grupo, fase = fase,
        equipoLocal = Equipo("Local", "LOC"),
        equipoVisitante = Equipo("Visitante", "VIS"),
        fecha = Instant.ofEpochMilli(Long.MAX_VALUE),
        sede = "Estadio",
        golesLocal = golesLocal, golesVisitante = golesVisitante,
        estado = estado, excluido = excluido,
    )

    private fun prediccionFake(partidoId: String = "p1", gL: Int = 1, gV: Int = 0) = Prediccion(
        uid = "u1", partidoId = partidoId,
        golesLocal = gL, golesVisitante = gV,
        creadaEn = Instant.EPOCH, actualizadaEn = Instant.EPOCH,
    )

    private fun configFake(puntosExacto: Int = 5, puntosTendencia: Int = 3) = ConfiguracionApp(
        nombreCompeticion = "Test", descripcion = "", temporada = "2026",
        codigoApi = "123", puntosExacto = puntosExacto, puntosTendencia = puntosTendencia,
        lockAnticipacionMin = 30, activa = true,
        creadaEn = Instant.EPOCH, actualizadaEn = Instant.EPOCH,
    )

    private fun runTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) =
        kotlinx.coroutines.test.runTest(testDispatcher, testBody = block)

    private fun kotlinx.coroutines.test.TestScope.obtenerSecciones(
        vm: PartidosViewModel,
    ): UiState<List<PartidoJornadaSeccion>> {
        val job = launch { vm.secciones.collect { } }
        val value = vm.secciones.value
        job.cancel()
        return value
    }

    private fun puntosDelPrimero(secciones: UiState<List<PartidoJornadaSeccion>>): Int? =
        (secciones as? UiState.Success)?.data
            ?.firstOrNull()?.grupos?.firstOrNull()?.items?.firstOrNull()?.puntosGanados

    // ── calcularPuntos ────────────────────────────────────────────────────────────

    @Test
    fun `partido no finalizado tiene puntosGanados null`() = runTest {
        val vm = crearViewModel(partidos = listOf(partidoFake(estado = EstadoPartido.PROGRAMADO)))
        assertNull(puntosDelPrimero(obtenerSecciones(vm)))
    }

    @Test
    fun `partido finalizado sin prediccion tiene puntosGanados null`() = runTest {
        val vm = crearViewModel(
            partidos = listOf(partidoFake(estado = EstadoPartido.FINALIZADO, golesLocal = 1, golesVisitante = 0)),
            predicciones = emptyList(),
        )
        assertNull(puntosDelPrimero(obtenerSecciones(vm)))
    }

    @Test
    fun `acierto exacto retorna puntosExacto`() = runTest {
        val vm = crearViewModel(
            partidos = listOf(partidoFake(estado = EstadoPartido.FINALIZADO, golesLocal = 2, golesVisitante = 1)),
            predicciones = listOf(prediccionFake(gL = 2, gV = 1)),
        )
        assertEquals(5, puntosDelPrimero(obtenerSecciones(vm)))
    }

    // ── mismoGanador ─────────────────────────────────────────────────────────────

    @Test
    fun `tendencia local gana retorna puntosTendencia`() = runTest {
        val vm = crearViewModel(
            partidos = listOf(partidoFake(estado = EstadoPartido.FINALIZADO, golesLocal = 3, golesVisitante = 0)),
            predicciones = listOf(prediccionFake(gL = 1, gV = 0)),
        )
        assertEquals(3, puntosDelPrimero(obtenerSecciones(vm)))
    }

    @Test
    fun `tendencia visitante gana retorna puntosTendencia`() = runTest {
        val vm = crearViewModel(
            partidos = listOf(partidoFake(estado = EstadoPartido.FINALIZADO, golesLocal = 0, golesVisitante = 2)),
            predicciones = listOf(prediccionFake(gL = 0, gV = 1)),
        )
        assertEquals(3, puntosDelPrimero(obtenerSecciones(vm)))
    }

    @Test
    fun `tendencia empate retorna puntosTendencia`() = runTest {
        val vm = crearViewModel(
            partidos = listOf(partidoFake(estado = EstadoPartido.FINALIZADO, golesLocal = 1, golesVisitante = 1)),
            predicciones = listOf(prediccionFake(gL = 2, gV = 2)),
        )
        assertEquals(3, puntosDelPrimero(obtenerSecciones(vm)))
    }

    @Test
    fun `fallo retorna cero puntos`() = runTest {
        val vm = crearViewModel(
            partidos = listOf(partidoFake(estado = EstadoPartido.FINALIZADO, golesLocal = 2, golesVisitante = 0)),
            predicciones = listOf(prediccionFake(gL = 0, gV = 1)),
        )
        assertEquals(0, puntosDelPrimero(obtenerSecciones(vm)))
    }

    // ── agruparEnSecciones ────────────────────────────────────────────────────────

    @Test
    fun `partido excluido no aparece en secciones`() = runTest {
        val vm = crearViewModel(partidos = listOf(partidoFake(excluido = true)))
        val secciones = obtenerSecciones(vm)
        val items = (secciones as? UiState.Success)?.data
            ?.flatMap { it.grupos }?.flatMap { it.items }
        assertTrue(items.isNullOrEmpty())
    }

    @Test
    fun `dos jornadas crean dos secciones`() = runTest {
        val vm = crearViewModel(
            partidos = listOf(
                partidoFake("p1", jornada = 1),
                partidoFake("p2", jornada = 2),
            ),
        )
        val secciones = obtenerSecciones(vm)
        assertEquals(2, (secciones as UiState.Success).data.size)
    }

    @Test
    fun `filtro por estado mantiene solo partidos del estado indicado`() = runTest {
        val vm = crearViewModel(
            partidos = listOf(
                partidoFake("p1", estado = EstadoPartido.PROGRAMADO),
                partidoFake("p2", estado = EstadoPartido.FINALIZADO, golesLocal = 1, golesVisitante = 0),
            ),
        )
        vm.filtrarPor(EstadoPartido.FINALIZADO)

        val items = (obtenerSecciones(vm) as UiState.Success).data
            .flatMap { it.grupos }.flatMap { it.items }

        assertEquals(1, items.size)
        assertEquals(EstadoPartido.FINALIZADO, items.first().partido.estado)
    }
}
