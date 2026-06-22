package com.example.quiniela_virtual_app.domain.usecase.prediccion

import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion.ABIERTO
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion.BLOQUEADO
import com.example.quiniela_virtual_app.domain.model.Partido
import javax.inject.Inject

class CalcularEstadoPrediccionUseCase @Inject constructor() {

    operator fun invoke(
        partido: Partido,
        ahoraMs: Long,
        lockMs: Long,
    ): EstadoPrediccion {
        if (partido.lockOverride == false) return evalUnlockTemporal(partido, ahoraMs)
        if (partido.lockOverride == true) return BLOQUEADO
        return evalPorEstado(partido, ahoraMs, lockMs)
    }

    private fun evalPorEstado(partido: Partido, ahoraMs: Long, lockMs: Long): EstadoPrediccion {
        val abiertoPorTiempo = partido.estado == EstadoPartido.PROGRAMADO ||
            partido.estado == EstadoPartido.SUSPENDIDO
        return if (abiertoPorTiempo) evalPorTiempo(partido, ahoraMs, lockMs) else BLOQUEADO
    }

    private fun evalUnlockTemporal(partido: Partido, ahoraMs: Long): EstadoPrediccion {
        val expiry = partido.lockOverrideExpiry ?: return BLOQUEADO
        return if (ahoraMs < expiry.toEpochMilli()) ABIERTO else BLOQUEADO
    }

    private fun evalPorTiempo(partido: Partido, ahoraMs: Long, lockMs: Long): EstadoPrediccion {
        val fechaMs = partido.fecha.toEpochMilli()
        return if (ahoraMs >= fechaMs - lockMs) BLOQUEADO else ABIERTO
    }
}
