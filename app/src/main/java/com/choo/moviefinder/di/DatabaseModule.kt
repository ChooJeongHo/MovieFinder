package com.choo.moviefinder.di

import android.content.Context
import androidx.room.Room
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MovieDatabase {
        return Room.databaseBuilder(
            context,
            MovieDatabase::class.java,
            "movie_finder_db"
        )
            // TODO: 프로덕션 배포 전 적절한 Migration으로 교체 필요 (즐겨찾기/검색 기록 보존)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideFavoriteMovieDao(database: MovieDatabase): FavoriteMovieDao {
        return database.favoriteMovieDao()
    }

    @Provides
    @Singleton
    fun provideRecentSearchDao(database: MovieDatabase): RecentSearchDao {
        return database.recentSearchDao()
    }

    @Provides
    @Singleton
    fun provideCachedMovieDao(database: MovieDatabase): CachedMovieDao {
        return database.cachedMovieDao()
    }

    @Provides
    @Singleton
    fun provideRemoteKeyDao(database: MovieDatabase): RemoteKeyDao {
        return database.remoteKeyDao()
    }
}