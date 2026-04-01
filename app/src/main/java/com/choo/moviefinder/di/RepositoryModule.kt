package com.choo.moviefinder.di

import com.choo.moviefinder.data.local.PreferencesRepositoryImpl
import com.choo.moviefinder.data.repository.MovieRepositoryImpl
import com.choo.moviefinder.data.repository.TagRepositoryImpl
import com.choo.moviefinder.domain.repository.BackupRepository
import com.choo.moviefinder.domain.repository.FavoriteRepository
import com.choo.moviefinder.domain.repository.MemoRepository
import com.choo.moviefinder.domain.repository.MovieRepository
import com.choo.moviefinder.domain.repository.PersonRepository
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.SearchHistoryRepository
import com.choo.moviefinder.domain.repository.TagRepository
import com.choo.moviefinder.domain.repository.UserRatingRepository
import com.choo.moviefinder.domain.repository.WatchHistoryRepository
import com.choo.moviefinder.domain.repository.WatchlistRepository
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

    // MovieRepositoryImpl을 FavoriteRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: MovieRepositoryImpl): FavoriteRepository

    // MovieRepositoryImpl을 WatchlistRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: MovieRepositoryImpl): WatchlistRepository

    // MovieRepositoryImpl을 SearchHistoryRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: MovieRepositoryImpl): SearchHistoryRepository

    // MovieRepositoryImpl을 WatchHistoryRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(impl: MovieRepositoryImpl): WatchHistoryRepository

    // MovieRepositoryImpl을 UserRatingRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindUserRatingRepository(impl: MovieRepositoryImpl): UserRatingRepository

    // MovieRepositoryImpl을 MemoRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindMemoRepository(impl: MovieRepositoryImpl): MemoRepository

    // MovieRepositoryImpl을 PersonRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindPersonRepository(impl: MovieRepositoryImpl): PersonRepository

    // MovieRepositoryImpl을 BackupRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: MovieRepositoryImpl): BackupRepository

    // PreferencesRepositoryImpl을 PreferencesRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    // TagRepositoryImpl을 TagRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}
