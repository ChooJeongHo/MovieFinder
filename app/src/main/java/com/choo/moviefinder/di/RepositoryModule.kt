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

    // MovieRepositoryImpl을 MovieRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    // PreferencesRepositoryImpl을 PreferencesRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}