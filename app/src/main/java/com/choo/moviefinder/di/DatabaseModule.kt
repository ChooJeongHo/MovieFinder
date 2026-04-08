package com.choo.moviefinder.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.MovieTagDao
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

    // 장르 정규화 테이블 추가 마이그레이션 (v12 → v13)
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `watch_history_genre` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `watch_history_id` INTEGER NOT NULL,
                    `genre_name` TEXT NOT NULL,
                    FOREIGN KEY(`watch_history_id`) REFERENCES `watch_history`(`id`) ON DELETE CASCADE
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_watch_history_genre_watch_history_id` " +
                    "ON `watch_history_genre` (`watch_history_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_watch_history_genre_genre_name` " +
                    "ON `watch_history_genre` (`genre_name`)"
            )
        }
    }

    // (watch_history_id, genre_name) UNIQUE 제약 추가 마이그레이션 (v13 → v14)
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `watch_history_genre_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `watch_history_id` INTEGER NOT NULL,
                    `genre_name` TEXT NOT NULL,
                    FOREIGN KEY(`watch_history_id`) REFERENCES `watch_history`(`id`) ON DELETE CASCADE,
                    UNIQUE(`watch_history_id`, `genre_name`)
                )"""
            )
            db.execSQL(
                "INSERT OR IGNORE INTO `watch_history_genre_new` (watch_history_id, genre_name) " +
                    "SELECT watch_history_id, genre_name FROM `watch_history_genre`"
            )
            db.execSQL("DROP TABLE `watch_history_genre`")
            db.execSQL("ALTER TABLE `watch_history_genre_new` RENAME TO `watch_history_genre`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_watch_history_genre_watch_history_id` " +
                    "ON `watch_history_genre` (`watch_history_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_watch_history_genre_genre_name` " +
                    "ON `watch_history_genre` (`genre_name`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_watch_history_genre_watch_history_id_genre_name` " +
                    "ON `watch_history_genre` (`watch_history_id`, `genre_name`)"
            )
        }
    }

    // Room 데이터베이스 인스턴스를 생성하여 제공한다
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MovieDatabase {
        return Room.databaseBuilder(
            context,
            MovieDatabase::class.java,
            "movie_finder_db"
        )
            .addMigrations(MIGRATION_12_13, MIGRATION_13_14)
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

    // 영화 태그 DAO를 제공한다
    @Provides
    @Singleton
    fun provideMovieTagDao(database: MovieDatabase): MovieTagDao {
        return database.movieTagDao()
    }
}
