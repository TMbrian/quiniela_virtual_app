package com.example.quiniela_virtual_app.data.remote.dto

import com.example.quiniela_virtual_app.domain.model.Equipo
import com.example.quiniela_virtual_app.domain.model.EstadoPartido
import com.example.quiniela_virtual_app.domain.model.Partido
import com.google.gson.annotations.SerializedName
import java.time.Instant

/**
 * Respuesta raíz del endpoint /competitions/{code}/matches de football-data.org v4.
 * [matches] usa emptyList() como fallback por si la API omite el campo.
 */
data class RespuestaPartidosApi(
    @SerializedName("matches") val matches: List<PartidoApiDto> = emptyList(),
)

/**
 * DTO de un partido tal como lo devuelve football-data.org v4.
 * Todos los campos son nullable para tolerar partidos de fases eliminatorias
 * donde los equipos aún no están definidos y la API envía null en homeTeam/awayTeam.
 */
data class PartidoApiDto(
    @SerializedName("id")       val id: Long?,
    @SerializedName("utcDate")  val utcDate: String?,
    @SerializedName("status")   val status: String?,
    @SerializedName("matchday") val matchday: Int?,
    @SerializedName("stage")    val stage: String?,
    @SerializedName("group")    val group: String?,
    @SerializedName("homeTeam") val homeTeam: EquipoApiDto?,
    @SerializedName("awayTeam") val awayTeam: EquipoApiDto?,
    @SerializedName("score")    val score: MarcadorApiDto?,
    @SerializedName("venue")    val venue: String?,
)

/**
 * DTO de equipo dentro de un partido.
 * Todos nullable: en fases eliminatorias sin equipos definidos la API devuelve
 * objetos con todos los campos en null (o directamente null en el objeto padre).
 */
data class EquipoApiDto(
    @SerializedName("name")      val name: String?,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("tla")       val tla: String?,
    @SerializedName("crest")     val crest: String?,
)

/**
 * DTO del marcador de un partido.
 * [fullTime] es nullable porque la API lo omite en partidos no finalizados.
 */
data class MarcadorApiDto(
    @SerializedName("fullTime") val fullTime: GolesApiDto?,
)

/**
 * DTO de goles al tiempo completo.
 * Ambos valores son nullable: la API los envía como null antes de que finalice el partido.
 */
data class GolesApiDto(
    @SerializedName("home") val home: Int?,
    @SerializedName("away") val away: Int?,
)

/**
 * Convierte [PartidoApiDto] al modelo de dominio [Partido].
 * Aplica fallbacks en todos los campos para evitar NullPointerException
 * ante respuestas incompletas de la API (ej. fases eliminatorias sin equipos asignados).
 */
fun PartidoApiDto.toDomain(): Partido = Partido(
    id              = id?.toString() ?: "",
    jornada         = matchday ?: 0,
    grupo           = group?.removePrefix("GROUP_") ?: "",
    fase            = stage?.toFase() ?: "",
    equipoLocal     = homeTeam?.toDomain() ?: equipoIndefinido(),
    equipoVisitante = awayTeam?.toDomain() ?: equipoIndefinido(),
    fecha           = utcDate?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?: Instant.now(),
    sede            = venue ?: "",
    golesLocal      = score?.fullTime?.home,
    golesVisitante  = score?.fullTime?.away,
    estado          = status.toEstadoPartido(),
    apiId           = id?.toString(),
)

/**
 * Convierte [EquipoApiDto] al modelo de dominio [Equipo].
 * Prioriza [shortName] sobre [name]; usa "Por definir" si ambos son null.
 */
private fun EquipoApiDto.toDomain(): Equipo = Equipo(
    nombre   = shortName ?: name ?: "Por definir",
    iso      = tla ?: "--",
    crestUrl = crest,
)
/**
 * Devuelve un [Equipo] placeholder para partidos donde el rival aún no está definido,
 * frecuente en fases eliminatorias del Mundial antes de que se jueguen los cruces.
 */
private fun equipoIndefinido(): Equipo = Equipo(
    nombre = "Por definir",
    iso    = "--",
)

/**
 * Mapea el string de estado de la API al enum [EstadoPartido] del dominio.
 * Receptor nullable: si [status] llega null se trata como PROGRAMADO.
 */
private fun String?.toEstadoPartido(): EstadoPartido = when (this) {
    "IN_PLAY", "PAUSED", "LIVE" -> EstadoPartido.EN_VIVO
    "FINISHED"                  -> EstadoPartido.FINALIZADO
    "POSTPONED", "CANCELLED",
    "SUSPENDED"                 -> EstadoPartido.SUSPENDIDO
    else                        -> EstadoPartido.PROGRAMADO
}

/**
 * Traduce el código de fase de la API a texto legible en español.
 * Receptor nullable para poder encadenarse desde [stage] que puede ser null.
 */
private fun String.toFase(): String = when (this) {
    "GROUP_STAGE"    -> "Grupos"
    "ROUND_OF_32"    -> "Dieciseisavos"
    "ROUND_OF_16"    -> "Octavos de final"
    "QUARTER_FINALS" -> "Cuartos de final"
    "SEMI_FINALS"    -> "Semifinal"
    "THIRD_PLACE"    -> "3er Lugar"
    "FINAL"          -> "Final"
    else             -> this
}