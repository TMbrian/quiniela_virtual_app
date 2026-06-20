package com.example.quiniela_virtual_app.di

import com.example.quiniela_virtual_app.data.repository.ConfigRepositoryImpl
import com.example.quiniela_virtual_app.data.repository.LeaderboardRepositoryImpl
import com.example.quiniela_virtual_app.data.repository.PartidoRepositoryImpl
import com.example.quiniela_virtual_app.data.repository.PrediccionRepositoryImpl
import com.example.quiniela_virtual_app.data.repository.UsuarioRepositoryImpl
import com.example.quiniela_virtual_app.domain.repository.ConfigRepository
import com.example.quiniela_virtual_app.domain.repository.LeaderboardRepository
import com.example.quiniela_virtual_app.domain.repository.PartidoRepository
import com.example.quiniela_virtual_app.domain.repository.PrediccionRepository
import com.example.quiniela_virtual_app.domain.repository.UsuarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindPartidoRepository(impl: PartidoRepositoryImpl): PartidoRepository

    @Binds @Singleton
    abstract fun bindPrediccionRepository(impl: PrediccionRepositoryImpl): PrediccionRepository

    @Binds @Singleton
    abstract fun bindUsuarioRepository(impl: UsuarioRepositoryImpl): UsuarioRepository

    @Binds @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds @Singleton
    abstract fun bindLeaderboardRepository(impl: LeaderboardRepositoryImpl): LeaderboardRepository
}
