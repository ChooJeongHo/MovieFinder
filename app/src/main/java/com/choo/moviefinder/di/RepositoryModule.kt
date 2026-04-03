package com.choo.moviefinder.di

import com.choo.moviefinder.data.local.PreferencesRepositoryImpl
import com.choo.moviefinder.data.repository.BackupRepositoryImpl
import com.choo.moviefinder.data.repository.FavoriteRepositoryImpl
import com.choo.moviefinder.data.repository.MemoRepositoryImpl
import com.choo.moviefinder.data.repository.MovieRepositoryImpl
import com.choo.moviefinder.data.repository.PersonRepositoryImpl
import com.choo.moviefinder.data.repository.SearchHistoryRepositoryImpl
import com.choo.moviefinder.data.repository.TagRepositoryImpl
import com.choo.moviefinder.data.repository.UserRatingRepositoryImpl
import com.choo.moviefinder.data.repository.WatchHistoryRepositoryImpl
import com.choo.moviefinder.data.repository.WatchlistRepositoryImpl
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

    // FavoriteRepositoryImpl을 FavoriteRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    // WatchlistRepositoryImpl을 WatchlistRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository

    // SearchHistoryRepositoryImpl을 SearchHistoryRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    // WatchHistoryRepositoryImpl을 WatchHistoryRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(impl: WatchHistoryRepositoryImpl): WatchHistoryRepository

    // UserRatingRepositoryImpl을 UserRatingRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindUserRatingRepository(impl: UserRatingRepositoryImpl): UserRatingRepository

    // MemoRepositoryImpl을 MemoRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindMemoRepository(impl: MemoRepositoryImpl): MemoRepository

    // PersonRepositoryImpl을 PersonRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindPersonRepository(impl: PersonRepositoryImpl): PersonRepository

    // BackupRepositoryImpl을 BackupRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    // PreferencesRepositoryImpl을 PreferencesRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    // TagRepositoryImpl을 TagRepository 인터페이스에 바인딩한다
    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}
