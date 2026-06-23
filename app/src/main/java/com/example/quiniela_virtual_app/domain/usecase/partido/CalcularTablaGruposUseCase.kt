package com.example.quiniela_virtual_app.domain.usecase.partido

import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.PosicionGrupo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalcularTablaGruposUseCase @Inject constructor(
    private val obtenerPartidos: ObtenerPartidosUseCase,
) {
    operator fun invoke(): Flow<Map<String, List<PosicionGrupo>>> =
        obtenerPartidos().map { calcular(it) }

    private fun calcular(partidos: List<Partido>): Map<String, List<PosicionGrupo>> {
        val acumulado = mutableMapOf<String, MutableMap<String, PosicionAcum>>()
        for (partido in partidos.filter { it.fase == FASE_GRUPOS && !it.excluido }) {
            if (partido.estado != EstadoPartido.FINALIZADO) continue
            val gL = partido.golesLocal ?: continue
            val gV = partido.golesVisitante ?: continue
            registrarPartido(acumulado, partido, gL, gV)
        }
        return acumulado.mapValues { (_, mapa) ->
            mapa.values.map { it.toPosicion() }.ordenadas()
        }.toSortedMap()
    }

    private fun registrarPartido(
        acumulado: MutableMap<String, MutableMap<String, PosicionAcum>>,
        partido: Partido,
        gL: Int,
        gV: Int,
    ) {
        val mapa = acumulado.getOrPut(partido.grupo) { mutableMapOf() }
        mapa.getOrPut(partido.equipoLocal.nombre) { PosicionAcum(partido.equipoLocal) }.registrar(gL, gV)
        mapa.getOrPut(partido.equipoVisitante.nombre) { PosicionAcum(partido.equipoVisitante) }.registrar(gV, gL)
    }

    private fun List<PosicionGrupo>.ordenadas(): List<PosicionGrupo> =
        sortedWith(
            compareByDescending<PosicionGrupo> { it.pts }
                .thenByDescending { it.dif }
                .thenByDescending { it.gf },
        )

    companion object {
        private const val FASE_GRUPOS = "Grupos"
    }
}

private class PosicionAcum(val equipo: Equipo) {
    var pj = 0; var g = 0; var e = 0; var p = 0; var gf = 0; var gc = 0

    fun registrar(propio: Int, contrario: Int) {
        pj++
        gf += propio
        gc += contrario
        when {
            propio > contrario  -> g++
            propio == contrario -> e++
            else                -> p++
        }
    }

    fun toPosicion() = PosicionGrupo(equipo, pj, g, e, p, gf, gc)
}
