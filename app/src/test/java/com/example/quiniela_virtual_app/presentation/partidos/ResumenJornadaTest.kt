package com.example.quiniela_virtual_app.presentation.partidos

import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.EstadoPrediccion
import com.example.quiniela_virtual_app.domain.model.Partido
import com.example.quiniela_virtual_app.domain.model.Prediccion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class ResumenJornadaTest {

    private fun item(
        estado: EstadoPartido = EstadoPartido.PROGRAMADO,
        prediccion: Prediccion? = null,
        puntosGanados: Int? = null,
    ) = PartidoConPrediccion(
        partido = Partido(
            id = "p1", jornada = 1, grupo = "A", fase = "Grupos",
            equipoLocal = Equipo("L", "LOC"), equipoVisitante = Equipo("V", "VIS"),
            fecha = Instant.EPOCH, sede = "E",
            golesLocal = null, golesVisitante = null, estado = estado,
        ),
        prediccion = prediccion,
        estadoPrediccion = EstadoPrediccion.BLOQUEADO,
        puntosGanados = puntosGanados,
    )

    private fun seccion(items: List<PartidoConPrediccion>) = PartidoJornadaSeccion(
        titulo = "Jornada 1", subtitulo = "Grupos",
        grupos = listOf(PartidoGrupoSeccion(nombre = "A", items = items)),
    )

    private fun prediccionFake() = Prediccion(
        uid = "u1", partidoId = "p1",
        golesLocal = 1, golesVisitante = 0,
        creadaEn = Instant.EPOCH, actualizadaEn = Instant.EPOCH,
    )

    @Test
    fun `seccion vacia retorna ceros en todos los campos`() {
        val resumen = resumenDe(seccion(emptyList()))

        assertEquals(0, resumen.total)
        assertEquals(0, resumen.conPrediccion)
        assertEquals(0, resumen.puntosGanados)
        assertFalse(resumen.hayFinalizados)
    }

    @Test
    fun `cuenta correctamente partidos con y sin prediccion`() {
        val items = listOf(
            item(prediccion = prediccionFake()),
            item(prediccion = null),
            item(prediccion = prediccionFake()),
        )
        val resumen = resumenDe(seccion(items))

        assertEquals(3, resumen.total)
        assertEquals(2, resumen.conPrediccion)
    }

    @Test
    fun `suma puntosGanados ignorando nulls`() {
        val items = listOf(
            item(puntosGanados = 5),
            item(puntosGanados = 3),
            item(puntosGanados = null),
        )
        val resumen = resumenDe(seccion(items))

        assertEquals(8, resumen.puntosGanados)
    }

    @Test
    fun `hayFinalizados es true cuando hay partido finalizado con prediccion`() {
        val items = listOf(
            item(estado = EstadoPartido.FINALIZADO, prediccion = prediccionFake()),
        )
        assertTrue(resumenDe(seccion(items)).hayFinalizados)
    }

    @Test
    fun `hayFinalizados es false si partido finalizado no tiene prediccion`() {
        val items = listOf(
            item(estado = EstadoPartido.FINALIZADO, prediccion = null),
        )
        assertFalse(resumenDe(seccion(items)).hayFinalizados)
    }
}
