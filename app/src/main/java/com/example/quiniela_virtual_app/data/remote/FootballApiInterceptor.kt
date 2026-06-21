package com.example.quiniela_virtual_app.data.remote

import com.example.quiniela_virtual_app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/** Agrega el header X-Auth-Token requerido por football-data.org en cada petición. */
class FootballApiInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-Auth-Token", BuildConfig.FOOTBALL_API_TOKEN)
            .build()
        return chain.proceed(request)
    }
}
