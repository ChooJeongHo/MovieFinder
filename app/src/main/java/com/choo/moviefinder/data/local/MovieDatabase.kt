package com.choo.moviefinder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.MovieTagDao
import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.CachedMovieEntity
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.data.local.entity.MovieTagEntity
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
import com.choo.moviefinder.data.local.entity.RemoteKeyEntity
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import com.choo.moviefinder.data.local.entity.WatchHistoryEntity
import com.choo.moviefinder.data.local.entity.WatchHistoryGenreEntity
import com.choo.moviefinder.data.local.entity.WatchlistEntity

@Database(
    entities = [
        FavoriteMovieEntity::class,
        RecentSearchEntity::class,
        CachedMovieEntity::class,
        RemoteKeyEntity::class,
        WatchHistoryEntity::class,
        WatchHistoryGenreEntity::class,
        WatchlistEntity::class,
        UserRatingEntity::class,
        MemoEntity::class,
        MovieTagEntity::class
    ],
    version = 14,
    exportSchema = true
)
abstract class MovieDatabase : RoomDatabase() {
    // 즐겨찾기 영화 DAO 제공
    abstract fun favoriteMovieDao(): FavoriteMovieDao

    // 최근 검색어 DAO 제공
    abstract fun recentSearchDao(): RecentSearchDao

    // 캐시 영화 DAO 제공
    abstract fun cachedMovieDao(): CachedMovieDao

    // 원격 페이징 키 DAO 제공
    abstract fun remoteKeyDao(): RemoteKeyDao

    // 시청 기록 DAO 제공
    abstract fun watchHistoryDao(): WatchHistoryDao

    // 워치리스트 DAO 제공
    abstract fun watchlistDao(): WatchlistDao

    // 사용자 평점 DAO 제공
    abstract fun userRatingDao(): UserRatingDao

    // 메모 DAO 제공
    abstract fun memoDao(): MemoDao

    // 영화 태그 DAO 제공
    abstract fun movieTagDao(): MovieTagDao
}
