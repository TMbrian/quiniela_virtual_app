package com.example.quiniela_virtual_app.domain.usecase.prediccion

import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion.ABIERTO
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion.BLOQUEADO
import com.example.quiniela_virtual_app.domain.model.Partido
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class CalcularEstadoPrediccionUseCaseTest {

    private val useCase = CalcularEstadoPrediccionUseCase()

    private val AHORA = 1_000_000L
    private val LOCK_MS = 30 * 60_000L   // 30 minutos
    private val UNA_HORA = 60 * 60_000L

    private fun partido(
        estado: EstadoPartido = EstadoPartido.PROGRAMADO,
        fechaMs: Long = AHORA + UNA_HORA,
        lockOverride: Boolean? = null,
        lockOverrideExpiry: Instant? = null,
    ) = Partido(
        id = "p1", jornada = 1, grupo = "A", fase = "Grupos",
        equipoLocal = Equipo("Local", "LOC"),
        equipoVisitante = Equipo("Visitante", "VIS"),
        fecha = Instant.ofEpochMilli(fechaMs),
        sede = "Estadio",
        golesLocal = null, golesVisitante = null,
        estado = estado,
        lockOverride = lockOverride,
        lockOverrideExpiry = lockOverrideExpiry,
    )

    // ── EN_VIVO ──────────────────────────────────────────────────────────────

    @Test
    fun `en vivo siempre bloqueado sin importar lockOverride`() {
        assertEquals(BLOQUEADO, useCase(partido(EstadoPartido.EN_VIVO), AHORA, LOCK_MS))
    }

    // ── FINALIZADO ───────────────────────────────────────────────────────────

    @Test
    fun `finalizado sin lockOverride queda bloqueado`() {
        assertEquals(BLOQUEADO, useCase(partido(EstadoPartido.FINALIZADO), AHORA, LOCK_MS))
    }

    @Test
    fun `finalizado con lockOverride true queda bloqueado`() {
        assertEquals(BLOQUEADO, useCase(partido(EstadoPartido.FINALIZADO, lockOverride = true), AHORA, LOCK_MS))
    }

    @Test
    fun `finalizado con unlock activo y expiry futuro queda abierto`() {
        val expiry = Instant.ofEpochMilli(AHORA + UNA_HORA)
        val p = partido(EstadoPartido.FINALIZADO, lockOverride = false, lockOverrideExpiry = expiry)
        assertEquals(ABIERTO, useCase(p, AHORA, LOCK_MS))
    }

    @Test
    fun `finalizado con unlock pero expiry ya paso queda bloqueado`() {
        val expiry = Instant.ofEpochMilli(AHORA - 1)
        val p = partido(EstadoPartido.FINALIZADO, lockOverride = false, lockOverrideExpiry = expiry)
        assertEquals(BLOQUEADO, useCase(p, AHORA, LOCK_MS))
    }

    @Test
    fun `finalizado con lockOverride false pero sin expiry queda bloqueado`() {
        val p = partido(EstadoPartido.FINALIZADO, lockOverride = false, lockOverrideExpiry = null)
        assertEquals(BLOQUEADO, useCase(p, AHORA, LOCK_MS))
    }

    // ── PROGRAMADO / SUSPENDIDO — lockOverride manual ────────────────────────

    @Test
    fun `programado con lockOverride true queda bloqueado`() {
        assertEquals(BLOQUEADO, useCase(partido(lockOverride = true), AHORA, LOCK_MS))
    }

    @Test
    fun `programado con unlock activo y expiry futuro queda abierto`() {
        val expiry = Instant.ofEpochMilli(AHORA + UNA_HORA)
        val p = partido(lockOverride = false, lockOverrideExpiry = expiry)
        assertEquals(ABIERTO, useCase(p, AHORA, LOCK_MS))
    }

    @Test
    fun `programado con unlock pero expiry ya paso queda bloqueado`() {
        val expiry = Instant.ofEpochMilli(AHORA - 1)
        val p = partido(lockOverride = false, lockOverrideExpiry = expiry)
        assertEquals(BLOQUEADO, useCase(p, AHORA, LOCK_MS))
    }

    @Test
    fun `programado con lockOverride false y sin expiry queda bloqueado`() {
        val p = partido(lockOverride = false, lockOverrideExpiry = null)
        assertEquals(BLOQUEADO, useCase(p, AHORA, LOCK_MS))
    }

    // ── PROGRAMADO — bloqueo automático por tiempo ───────────────────────────

    @Test
    fun `programado fuera de ventana de bloqueo queda abierto`() {
        // Partido en 2 horas, ventana 30 min → todavía abierto
        val p = partido(fechaMs = AHORA + UNA_HORA * 2)
        assertEquals(ABIERTO, useCase(p, AHORA, LOCK_MS))
    }

    @Test
    fun `programado dentro de ventana de bloqueo queda bloqueado`() {
        // Partido en 15 min, ventana 30 min → bloqueado
        val p = partido(fechaMs = AHORA + 15 * 60_000L)
        assertEquals(BLOQUEADO, useCase(p, AHORA, LOCK_MS))
    }

    @Test
    fun `programado exactamente en el limite de la ventana queda bloqueado`() {
        // ahoraMs == fechaMs - lockMs → bloqueado (condición >=)
        val p = partido(fechaMs = AHORA + LOCK_MS)
        assertEquals(BLOQUEADO, useCase(p, AHORA, LOCK_MS))
    }

    @Test
    fun `suspendido fuera de ventana se comporta igual que programado`() {
        val p = partido(estado = EstadoPartido.SUSPENDIDO, fechaMs = AHORA + UNA_HORA * 2)
        assertEquals(ABIERTO, useCase(p, AHORA, LOCK_MS))
    }
}
