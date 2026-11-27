package com.choo.moviefinder.data.repository

import androidx.room.withTransaction
import com.choo.moviefinder.data.local.MovieDatabase
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.MovieTagDao
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.data.local.entity.MovieTagEntity
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import com.choo.moviefinder.data.local.entity.WatchHistoryEntity
import com.choo.moviefinder.data.local.entity.WatchHistoryGenreEntity
import com.choo.moviefinder.data.local.entity.WatchlistEntity
import com.choo.moviefinder.data.local.entity.toBackupMovie
import com.choo.moviefinder.domain.model.BackupMemo
import com.choo.moviefinder.domain.model.BackupMovie
import com.choo.moviefinder.domain.model.BackupRating
import com.choo.moviefinder.domain.model.BackupTag
import com.choo.moviefinder.domain.model.BackupWatchHistory
import com.choo.moviefinder.domain.model.UserDataBackup
import com.choo.moviefinder.domain.repository.BackupRepository
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val database: MovieDatabase,
    private val favoriteMovieDao: FavoriteMovieDao,
    private val watchlistDao: WatchlistDao,
    private val userRatingDao: UserRatingDao,
    private val memoDao: MemoDao,
    private val movieTagDao: MovieTagDao,
    private val watchHistoryDao: WatchHistoryDao
) : BackupRepository {

    // 즐겨찾기, 워치리스트, 평점, 메모, 태그, 시청기록을 백업 모델로 내보낸다
    override suspend fun exportUserData(): UserDataBackup = database.withTransaction {
        val favorites = favoriteMovieDao.getAllFavoritesOnce().map { it.toBackupMovie() }
        val watchlist = watchlistDao.getAllWatchlistOnce().map { it.toBackupMovie() }
        val ratings = userRatingDao.getAllRatings().map { entity ->
            BackupRating(movieId = entity.movieId, rating = entity.rating)
        }
        val memos = memoDao.getAllMemos().map { entity ->
            BackupMemo(
                movieId = entity.movieId,
                content = entity.content,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
        val tags = movieTagDao.getAllTagsOnce().map { entity ->
            BackupTag(
                movieId = entity.movieId,
                tagName = entity.tagName,
                addedAt = entity.addedAt
            )
        }
        val watchHistory = watchHistoryDao.getAllHistoryOnce().map { entity ->
            BackupWatchHistory(
                movieId = entity.movieId,
                title = entity.title,
                posterPath = entity.posterPath,
                backdropPath = entity.backdropPath,
                overview = entity.overview,
                releaseDate = entity.releaseDate,
                voteAverage = entity.voteAverage,
                voteCount = entity.voteCount,
                watchedAt = entity.watchedAt,
                genres = entity.genres
            )
        }
        UserDataBackup(
            favorites = favorites,
            watchlist = watchlist,
            ratings = ratings,
            memos = memos,
            tags = tags,
            watchHistory = watchHistory
        )
    }

    // 백업 데이터를 기존 데이터와 병합한다 (중복 시 덮어쓰기, 원자적 트랜잭션). 단, 시청 기록은 키(movieId, watchedAt, title)가 같으면 덮어쓰지 않고 건너뛴다
    override suspend fun importUserData(backup: UserDataBackup) {
        database.withTransaction {
            restoreFavorites(backup.favorites)
            restoreWatchlist(backup.watchlist)
            restoreRatings(backup.ratings)
            restoreMemos(backup.memos)
            restoreTags(backup.tags)
            restoreWatchHistory(backup.watchHistory)
        }
    }

    private suspend fun restoreFavorites(favorites: List<BackupMovie>) {
        val entities = favorites.map { movie ->
            FavoriteMovieEntity(
                id = movie.id, title = movie.title, posterPath = movie.posterPath,
                backdropPath = movie.backdropPath, overview = movie.overview,
                releaseDate = movie.releaseDate, voteAverage = movie.voteAverage,
                voteCount = movie.voteCount,
                addedAt = if (movie.addedAt != 0L) movie.addedAt else System.currentTimeMillis()
            )
        }
        if (entities.isNotEmpty()) favoriteMovieDao.insertAll(entities)
    }

    private suspend fun restoreWatchlist(watchlist: List<BackupMovie>) {
        val entities = watchlist.map { movie ->
            WatchlistEntity(
                id = movie.id, title = movie.title, posterPath = movie.posterPath,
                backdropPath = movie.backdropPath, overview = movie.overview,
                releaseDate = movie.releaseDate, voteAverage = movie.voteAverage,
                voteCount = movie.voteCount,
                addedAt = if (movie.addedAt != 0L) movie.addedAt else System.currentTimeMillis()
            )
        }
        if (entities.isNotEmpty()) watchlistDao.insertAll(entities)
    }

    private suspend fun restoreRatings(ratings: List<BackupRating>) {
        val entities = ratings.map { UserRatingEntity(movieId = it.movieId, rating = it.rating) }
        if (entities.isNotEmpty()) userRatingDao.insertAll(entities)
    }

    private suspend fun restoreMemos(memos: List<BackupMemo>) {
        val now = System.currentTimeMillis()
        val entities = memos.map { memo ->
            MemoEntity(
                movieId = memo.movieId, content = memo.content,
                createdAt = if (memo.createdAt != 0L) memo.createdAt else now,
                updatedAt = if (memo.updatedAt != 0L) memo.updatedAt else now
            )
        }
        if (entities.isNotEmpty()) memoDao.insertAll(entities)
    }

    private suspend fun restoreTags(tags: List<BackupTag>) {
        val entities = tags.map { tag ->
            MovieTagEntity(
                movieId = tag.movieId, tagName = tag.tagName,
                addedAt = if (tag.addedAt != 0L) tag.addedAt else System.currentTimeMillis()
            )
        }
        if (entities.isNotEmpty()) movieTagDao.insertAll(entities)
    }

    private suspend fun restoreWatchHistory(history: List<BackupWatchHistory>) {
        if (history.isEmpty()) return
        // 같은 시청 기록(영화 ID·시청 시각·제목)의 중복 삽입을 건너뛴다. 백업 안에서 겹치면 첫 항목만 남긴다
        val storedKeys = watchHistoryDao.getAllHistoryOnce()
            .map { Triple(it.movieId, it.watchedAt, it.title) }
            .toSet()
        val newItems = history
            .distinctBy { Triple(it.movieId, it.watchedAt, it.title) }
            .filterNot { Triple(it.movieId, it.watchedAt, it.title) in storedKeys }
        val entities = newItems.map { item ->
            WatchHistoryEntity(
                movieId = item.movieId,
                title = item.title,
                posterPath = item.posterPath,
                backdropPath = item.backdropPath,
                overview = item.overview,
                releaseDate = item.releaseDate,
                voteAverage = item.voteAverage,
                voteCount = item.voteCount,
                watchedAt = item.watchedAt,
                yearMonth = item.watchedAt.toYearMonth(),
                genres = item.genres
            )
        }
        if (entities.isEmpty()) return
        val rowIds = watchHistoryDao.insertAll(entities)
        // 평소 저장 경로(saveWatchHistoryWithGenres)와 같은 분리 규칙. rowId -1(충돌로 무시됨)은 장르 행을 만들지 않는다
        val genres = rowIds.zip(entities)
            .filter { (rowId, _) -> rowId != -1L }
            .flatMap { (rowId, entity) ->
                entity.genres.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { WatchHistoryGenreEntity(watchHistoryId = rowId, genreName = it) }
            }
        if (genres.isNotEmpty()) watchHistoryDao.insertGenres(genres)
    }
}

private fun Long.toYearMonth(): String {
    val date = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${date.year}-${date.month.number.toString().padStart(2, '0')}"
}
