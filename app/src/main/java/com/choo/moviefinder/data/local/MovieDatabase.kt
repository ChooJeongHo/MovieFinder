package com.choo.moviefinder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.entity.CachedMovieEntity
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
import com.choo.moviefinder.data.local.entity.RemoteKeyEntity

@Database(
    entities = [
        FavoriteMovieEntity::class,
        RecentSearchEntity::class,
        CachedMovieEntity::class,
        RemoteKeyEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun favoriteMovieDao(): FavoriteMovieDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun cachedMovieDao(): CachedMovieDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}
