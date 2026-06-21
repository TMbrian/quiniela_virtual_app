package com.example.quiniela_virtual_app.data.remote.api

import com.example.quiniela_virtual_app.data.remote.dto.RespuestaPartidosApi
import retrofit2.http.GET
import retrofit2.http.Path

/** Interfaz Retrofit para el endpoint de partidos de football-data.org v4. */
interface FootballApiService {

    /** Devuelve todos los partidos de la competición indicada (ej. "WC" para Mundial). */
    @GET("competitions/{codigo}/matches")
    suspend fun obtenerPartidos(
        @Path("codigo") codigoCompeticion: String,
    ): RespuestaPartidosApi
}
