package com.choo.moviefinder.di

import com.choo.moviefinder.data.local.PreferencesRepositoryImpl
import com.choo.moviefinder.data.local.TokenRepositoryImpl
import com.choo.moviefinder.data.repository.BackupRepositoryImpl
import com.choo.moviefinder.data.repository.FavoriteRepositoryImpl
import com.choo.moviefinder.data.repository.MemoRepositoryImpl
import com.choo.moviefinder.data.repository.PersonRepositoryImpl
import com.choo.moviefinder.data.repository.ReminderRepositoryImpl
import com.choo.moviefinder.data.repository.SearchHistoryRepositoryImpl
import com.choo.moviefinder.data.repository.TagRepositoryImpl
import com.choo.moviefinder.data.repository.TmdbAuthRepositoryImpl
import com.choo.moviefinder.data.repository.UserRatingRepositoryImpl
import com.choo.moviefinder.data.repository.WatchHistoryRepositoryImpl
import com.choo.moviefinder.data.repository.WatchlistRepositoryImpl
import com.choo.moviefinder.domain.repository.BackupRepository
import com.choo.moviefinder.domain.repository.FavoriteRepository
import com.choo.moviefinder.domain.repository.MemoRepository
import com.choo.moviefinder.domain.repository.PersonRepository
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.ReminderRepository
import com.choo.moviefinder.domain.repository.SearchHistoryRepository
import com.choo.moviefinder.domain.repository.TagRepository
import com.choo.moviefinder.domain.repository.TmdbAuthRepository
import com.choo.moviefinder.domain.repository.TokenRepository
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

    @Binds @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository

    @Binds @Singleton
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    @Binds @Singleton
    abstract fun bindWatchHistoryRepository(impl: WatchHistoryRepositoryImpl): WatchHistoryRepository

    @Binds @Singleton
    abstract fun bindUserRatingRepository(impl: UserRatingRepositoryImpl): UserRatingRepository

    @Binds @Singleton
    abstract fun bindMemoRepository(impl: MemoRepositoryImpl): MemoRepository

    @Binds @Singleton
    abstract fun bindPersonRepository(impl: PersonRepositoryImpl): PersonRepository

    @Binds @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds @Singleton
    abstract fun bindTokenRepository(impl: TokenRepositoryImpl): TokenRepository

    @Binds @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds @Singleton
    abstract fun bindTmdbAuthRepository(impl: TmdbAuthRepositoryImpl): TmdbAuthRepository
}
