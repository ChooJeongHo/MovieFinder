package com.choo.moviefinder.di

import com.choo.moviefinder.data.repository.MovieRepositoryImpl
import com.choo.moviefinder.domain.repository.MovieDetailRepository
import com.choo.moviefinder.domain.repository.MovieQueryRepository
import com.choo.moviefinder.domain.repository.MovieRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MovieRepositoryModule {

    @Binds @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds @Singleton
    abstract fun bindMovieQueryRepository(impl: MovieRepositoryImpl): MovieQueryRepository

    @Binds @Singleton
    abstract fun bindMovieDetailRepository(impl: MovieRepositoryImpl): MovieDetailRepository
}
