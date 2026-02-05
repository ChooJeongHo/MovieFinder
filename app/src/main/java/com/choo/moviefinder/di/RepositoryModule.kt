package com.choo.moviefinder.di

import com.choo.moviefinder.data.local.PreferencesRepositoryImpl
import com.choo.moviefinder.data.repository.MovieRepositoryImpl
import com.choo.moviefinder.domain.repository.MovieRepository
import com.choo.moviefinder.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}