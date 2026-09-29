package com.choo.moviefinder.di

import com.choo.moviefinder.data.repository.LocalizedTitleRepositoryImpl
import com.choo.moviefinder.data.repository.MovieRepositoryImpl
import com.choo.moviefinder.domain.repository.LocalizedTitleRepository
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

    // 표시 제목 메모리 LRU 캐시를 가지므로 반드시 싱글톤이어야 한다 (@Singleton 없으면 주입처마다 캐시가 따로 생김)
    @Binds @Singleton
    abstract fun bindLocalizedTitleRepository(impl: LocalizedTitleRepositoryImpl): LocalizedTitleRepository
}
