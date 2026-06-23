package com.example.quiniela_virtual_app.domain.model

data class PosicionGrupo(
    val equipo: Equipo,
    val pj: Int,
    val g: Int,
    val e: Int,
    val p: Int,
    val gf: Int,
    val gc: Int,
) {
    val dif: Int get() = gf - gc
    val pts: Int get() = g * 3 + e
}
