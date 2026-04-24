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
import com.choo.moviefinder.data.local.dao.ScheduledReminderDao
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

    // composite 인덱스 추가 마이그레이션 (v14 → v15)
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_cached_movies_category_page_cachedAt` " +
                    "ON `cached_movies` (`category`, `page`, `cachedAt`)"
            )
        }
    }

    // favorite_movies/watchlist_movies title+voteAverage 인덱스, user_ratings rating 인덱스 추가 (v15 → v16)
    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_favorite_movies_title` " +
                    "ON `favorite_movies`(`title`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_favorite_movies_voteAverage` " +
                    "ON `favorite_movies`(`voteAverage`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_watchlist_movies_title` " +
                    "ON `watchlist_movies`(`title`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_watchlist_movies_voteAverage` " +
                    "ON `watchlist_movies`(`voteAverage`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_user_ratings_rating` " +
                    "ON `user_ratings`(`rating`)"
            )
        }
    }

    // watch_history yearMonth 컬럼 추가 및 인덱스 생성 마이그레이션 (v16 → v17)
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE watch_history ADD COLUMN yearMonth TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "UPDATE watch_history SET yearMonth = " +
                    "strftime('%Y-%m', watchedAt / 1000, 'unixepoch', 'localtime')"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_watch_history_yearMonth` " +
                    "ON `watch_history` (`yearMonth`)"
            )
        }
    }

    // watchlist_movies reminderDate 컬럼 추가 마이그레이션 (v17 → v18)
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE watchlist_movies ADD COLUMN reminderDate INTEGER"
            )
        }
    }

    // scheduled_reminders 테이블 추가 마이그레이션 (v18 → v19)
    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `scheduled_reminders` " +
                    "(`movieId` INTEGER NOT NULL, " +
                    "`movieTitle` TEXT NOT NULL, " +
                    "`releaseDate` TEXT NOT NULL, " +
                    "`scheduledAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`movieId`))"
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
            .addMigrations(
                MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
                MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19
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

    // 영화 태그 DAO를 제공한다
    @Provides
    @Singleton
    fun provideMovieTagDao(database: MovieDatabase): MovieTagDao {
        return database.movieTagDao()
    }

    // 예약된 알림 DAO를 제공한다
    @Provides
    @Singleton
    fun provideScheduledReminderDao(database: MovieDatabase): ScheduledReminderDao {
        return database.scheduledReminderDao()
    }
}
