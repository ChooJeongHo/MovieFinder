package com.choo.moviefinder.di

import android.content.Context
import androidx.room.Room
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Room 데이터베이스 인스턴스를 생성하여 제공한다
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MovieDatabase {
        return Room.databaseBuilder(
            context,
            MovieDatabase::class.java,
            "movie_finder_db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // 즐겨찾기 영화 DAO를 제공한다
    @Provides
    @Singleton
    fun provideFavoriteMovieDao(database: MovieDatabase): FavoriteMovieDao {
        return database.favoriteMovieDao()
    }

    // 최근 검색어 DAO를 제공한다
    @Provides
    @Singleton
    fun provideRecentSearchDao(database: MovieDatabase): RecentSearchDao {
        return database.recentSearchDao()
    }

    // 캐시된 영화 DAO를 제공한다
    @Provides
    @Singleton
    fun provideCachedMovieDao(database: MovieDatabase): CachedMovieDao {
        return database.cachedMovieDao()
    }

    // 페이징 원격 키 DAO를 제공한다
    @Provides
    @Singleton
    fun provideRemoteKeyDao(database: MovieDatabase): RemoteKeyDao {
        return database.remoteKeyDao()
    }

    // 시청 기록 DAO를 제공한다
    @Provides
    @Singleton
    fun provideWatchHistoryDao(database: MovieDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    // 워치리스트 DAO를 제공한다
    @Provides
    @Singleton
    fun provideWatchlistDao(database: MovieDatabase): WatchlistDao {
        return database.watchlistDao()
    }

    // 사용자 평점 DAO를 제공한다
    @Provides
    @Singleton
    fun provideUserRatingDao(database: MovieDatabase): UserRatingDao {
        return database.userRatingDao()
    }

    // 메모 DAO를 제공한다
    @Provides
    @Singleton
    fun provideMemoDao(database: MovieDatabase): MemoDao {
        return database.memoDao()
    }
}
