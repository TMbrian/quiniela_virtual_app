package com.example.quiniela_virtual_app.data.repository

import com.example.quiniela_virtual_app.data.dto.ConfiguracionAppDto
import com.example.quiniela_virtual_app.data.dto.PartidoDto
import com.example.quiniela_virtual_app.data.dto.PosicionLeaderboardDto
import com.example.quiniela_virtual_app.data.dto.PrediccionDto
import com.example.quiniela_virtual_app.data.dto.UsuarioDto
import com.example.quiniela_virtual_app.data.source.firestore.ConfigFirestoreSource
import com.example.quiniela_virtual_app.data.source.firestore.LeaderboardFirestoreSource
import com.example.quiniela_virtual_app.data.source.firestore.PartidoFirestoreSource
import com.example.quiniela_virtual_app.data.source.firestore.PrediccionFirestoreSource
import com.example.quiniela_virtual_app.data.source.firestore.UsuarioFirestoreSource
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LeaderboardRepositoryImplTest {

    private val leaderboardSource = mockk<LeaderboardFirestoreSource>()
    private val partidoSource     = mockk<PartidoFirestoreSource>()
    private val prediccionSource  = mockk<PrediccionFirestoreSource>()
    private val usuarioSource     = mockk<UsuarioFirestoreSource>()
    private val configSource      = mockk<ConfigFirestoreSource>()

    private val repo = LeaderboardRepositoryImpl(
        leaderboardSource, partidoSource, prediccionSource, usuarioSource, configSource,
    )

    private val ts     = Timestamp(0L, 0)
    private val config = ConfiguracionAppDto(puntosExacto = 5, puntosTendencia = 3,
                                              creadaEn = ts, actualizadaEn = ts)
    private val usuario = UsuarioDto(uid = "u1", activo = true, creadoEn = ts, ultimoAcceso = ts)

    private fun partido(
        id: String = "p1",
        jornada: Int = 1,
        golesLocal: Long? = 1L,
        golesVisitante: Long? = 0L,
        estado: String = "FINALIZADO",
        excluido: Boolean = false,
    ) = PartidoDto(id = id, jornada = jornada, golesLocal = golesLocal,
                   golesVisitante = golesVisitante, estado = estado,
                   excluido = excluido, fecha = ts)

    private fun prediccion(partidoId: String = "p1", gL: Long = 1L, gV: Long = 0L) =
        PrediccionDto(partidoId = partidoId, uid = "u1", golesLocal = gL, golesVisitante = gV,
                      creadaEn = ts, actualizadaEn = ts)

    private val posicionSlot = slot<List<PosicionLeaderboardDto>>()

    @BeforeEach
    fun setUp() {
        coEvery { configSource.obtenerSnapshot() } returns config
        coEvery { usuarioSource.obtenerSnapshot() } returns listOf(usuario)
        coEvery { leaderboardSource.recalcular(capture(posicionSlot)) } returns Result.success(Unit)
    }

    private suspend fun recalcularYObtener(): PosicionLeaderboardDto {
        repo.recalcular()
        return posicionSlot.captured.first()
    }

    // ── Lógica de puntos ─────────────────────────────────────────────────────

    @Test
    fun `acierto exacto suma puntosExacto`() = runTest {
        coEvery { partidoSource.obtenerSnapshot() } returns listOf(partido(golesLocal = 2L, golesVisitante = 1L))
        coEvery { prediccionSource.obtenerSnapshot("u1") } returns listOf(prediccion(gL = 2L, gV = 1L))

        val posicion = recalcularYObtener()

        assertEquals(5L, posicion.puntosTotales)
        assertEquals(1L, posicion.aciertosExactos)
        assertEquals(0L, posicion.aciertosTendencia)
    }

    @Test
    fun `acierto por tendencia suma puntosTendencia`() = runTest {
        // Partido 2-0, predicción 1-0 → mismo ganador, marcador distinto
        coEvery { partidoSource.obtenerSnapshot() } returns listOf(partido(golesLocal = 2L, golesVisitante = 0L))
        coEvery { prediccionSource.obtenerSnapshot("u1") } returns listOf(prediccion(gL = 1L, gV = 0L))

        val posicion = recalcularYObtener()

        assertEquals(3L, posicion.puntosTotales)
        assertEquals(0L, posicion.aciertosExactos)
        assertEquals(1L, posicion.aciertosTendencia)
    }

    @Test
    fun `tendencia en empate suma puntosTendencia`() = runTest {
        // Partido 0-0, predicción 1-1 → ambos empate, no exacto
        coEvery { partidoSource.obtenerSnapshot() } returns listOf(partido(golesLocal = 0L, golesVisitante = 0L))
        coEvery { prediccionSource.obtenerSnapshot("u1") } returns listOf(prediccion(gL = 1L, gV = 1L))

        val posicion = recalcularYObtener()

        assertEquals(3L, posicion.puntosTotales)
        assertEquals(1L, posicion.aciertosTendencia)
    }

    @Test
    fun `prediccion incorrecta suma cero puntos`() = runTest {
        // Partido 2-0 (local gana), predicción 0-1 (visitante gana)
        coEvery { partidoSource.obtenerSnapshot() } returns listOf(partido(golesLocal = 2L, golesVisitante = 0L))
        coEvery { prediccionSource.obtenerSnapshot("u1") } returns listOf(prediccion(gL = 0L, gV = 1L))

        val posicion = recalcularYObtener()

        assertEquals(0L, posicion.puntosTotales)
        assertEquals(0L, posicion.aciertosExactos)
        assertEquals(0L, posicion.aciertosTendencia)
    }

    // ── Exclusiones ───────────────────────────────────────────────────────────

    @Test
    fun `partido excluido no suma puntos`() = runTest {
        coEvery { partidoSource.obtenerSnapshot() } returns
            listOf(partido(golesLocal = 2L, golesVisitante = 1L, excluido = true))
        coEvery { prediccionSource.obtenerSnapshot("u1") } returns listOf(prediccion(gL = 2L, gV = 1L))

        val posicion = recalcularYObtener()

        assertEquals(0L, posicion.puntosTotales)
        assertEquals(0L, posicion.aciertosExactos)
    }

    @Test
    fun `partido no finalizado no entra en el calculo`() = runTest {
        coEvery { partidoSource.obtenerSnapshot() } returns
            listOf(partido(golesLocal = 2L, golesVisitante = 1L, estado = "EN_VIVO"))
        coEvery { prediccionSource.obtenerSnapshot("u1") } returns listOf(prediccion(gL = 2L, gV = 1L))

        val posicion = recalcularYObtener()

        assertEquals(0L, posicion.puntosTotales)
    }

    @Test
    fun `usuario sin prediccion en un partido no suma puntos`() = runTest {
        coEvery { partidoSource.obtenerSnapshot() } returns listOf(partido())
        coEvery { prediccionSource.obtenerSnapshot("u1") } returns emptyList()

        val posicion = recalcularYObtener()

        assertEquals(0L, posicion.puntosTotales)
        assertEquals(0L, posicion.prediccionesTotales)
    }

    // ── Acumulación ───────────────────────────────────────────────────────────

    @Test
    fun `multiple partidos acumula puntos y desglosa por jornada`() = runTest {
        val partidos = listOf(
            partido("p1", jornada = 1, golesLocal = 1L, golesVisitante = 0L),  // local gana
            partido("p2", jornada = 1, golesLocal = 0L, golesVisitante = 0L),  // empate
            partido("p3", jornada = 2, golesLocal = 3L, golesVisitante = 2L),  // local gana
        )
        val predicciones = listOf(
            prediccion("p1", gL = 1L, gV = 0L),  // exacto      → 5 pts jornada 1
            prediccion("p2", gL = 0L, gV = 1L),  // fallo       → 0 pts jornada 1
            prediccion("p3", gL = 2L, gV = 1L),  // tendencia   → 3 pts jornada 2
        )
        coEvery { partidoSource.obtenerSnapshot() } returns partidos
        coEvery { prediccionSource.obtenerSnapshot("u1") } returns predicciones

        val posicion = recalcularYObtener()

        assertEquals(8L, posicion.puntosTotales)
        assertEquals(5L, posicion.puntosPorJornada["1"])
        assertEquals(3L, posicion.puntosPorJornada["2"])
        assertEquals(1L, posicion.aciertosExactos)
        assertEquals(1L, posicion.aciertosTendencia)
        assertEquals(3L, posicion.prediccionesTotales)
    }

    // ── Casos de error ────────────────────────────────────────────────────────

    @Test
    fun `sin configuracion retorna failure`() = runTest {
        coEvery { configSource.obtenerSnapshot() } returns null
        coEvery { partidoSource.obtenerSnapshot() } returns emptyList()
        coEvery { prediccionSource.obtenerSnapshot(any()) } returns emptyList()

        val result = repo.recalcular()

        assertTrue(result.isFailure)
    }
}
