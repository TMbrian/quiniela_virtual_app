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
        if (partido.estado == EstadoPartido.EN_VIVO) return BLOQUEADO
        if (partido.estado == EstadoPartido.FINALIZADO) return evalFinalizado(partido, ahoraMs)
        if (partido.lockOverride == true) return BLOQUEADO
        if (partido.lockOverride == false) return evalUnlockTemporal(partido, ahoraMs)
        return evalPorTiempo(partido, ahoraMs, lockMs)
    }

    private fun evalFinalizado(partido: Partido, ahoraMs: Long): EstadoPrediccion {
        val unlockActivo = partido.lockOverride == false
            && partido.lockOverrideExpiry != null
            && ahoraMs < partido.lockOverrideExpiry.toEpochMilli()
        return if (unlockActivo) ABIERTO else BLOQUEADO
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
