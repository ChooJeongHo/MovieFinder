package com.choo.moviefinder.data.repository

import androidx.room.withTransaction
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.MovieTagDao
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.data.local.entity.MovieTagEntity
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import com.choo.moviefinder.data.local.entity.WatchlistEntity
import com.choo.moviefinder.domain.model.BackupMemo
import com.choo.moviefinder.domain.model.BackupMovie
import com.choo.moviefinder.domain.model.BackupRating
import com.choo.moviefinder.domain.model.BackupTag
import com.choo.moviefinder.domain.model.UserDataBackup
import com.choo.moviefinder.domain.repository.BackupRepository
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val database: MovieDatabase,
    private val favoriteMovieDao: FavoriteMovieDao,
    private val watchlistDao: WatchlistDao,
    private val userRatingDao: UserRatingDao,
    private val memoDao: MemoDao,
    private val movieTagDao: MovieTagDao
) : BackupRepository {

    // 즐겨찾기, 워치리스트, 평점, 메모, 태그를 백업 모델로 내보낸다
    override suspend fun exportUserData(): UserDataBackup {
        val favorites = favoriteMovieDao.getAllFavoritesOnce().map { entity ->
            BackupMovie(
                id = entity.id,
                title = entity.title,
                posterPath = entity.posterPath,
                voteAverage = entity.voteAverage,
                overview = entity.overview,
                releaseDate = entity.releaseDate,
                backdropPath = entity.backdropPath,
                voteCount = entity.voteCount,
                addedAt = entity.addedAt
            )
        }
        val watchlist = watchlistDao.getAllWatchlistOnce().map { entity ->
            BackupMovie(
                id = entity.id,
                title = entity.title,
                posterPath = entity.posterPath,
                voteAverage = entity.voteAverage,
                overview = entity.overview,
                releaseDate = entity.releaseDate,
                backdropPath = entity.backdropPath,
                voteCount = entity.voteCount,
                addedAt = entity.addedAt
            )
        }
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
        return UserDataBackup(
            favorites = favorites,
            watchlist = watchlist,
            ratings = ratings,
            memos = memos,
            tags = tags
        )
    }

    // 백업 데이터를 기존 데이터와 병합한다 (중복 시 덮어쓰기, 원자적 트랜잭션)
    override suspend fun importUserData(backup: UserDataBackup) {
        database.withTransaction {
            val favoriteEntities = backup.favorites.map { movie ->
                FavoriteMovieEntity(
                    id = movie.id,
                    title = movie.title,
                    posterPath = movie.posterPath,
                    backdropPath = movie.backdropPath,
                    overview = movie.overview,
                    releaseDate = movie.releaseDate,
                    voteAverage = movie.voteAverage,
                    voteCount = movie.voteCount,
                    addedAt = if (movie.addedAt != 0L) movie.addedAt else System.currentTimeMillis()
                )
            }
            if (favoriteEntities.isNotEmpty()) {
                favoriteMovieDao.insertAll(favoriteEntities)
            }

            val watchlistEntities = backup.watchlist.map { movie ->
                WatchlistEntity(
                    id = movie.id,
                    title = movie.title,
                    posterPath = movie.posterPath,
                    backdropPath = movie.backdropPath,
                    overview = movie.overview,
                    releaseDate = movie.releaseDate,
                    voteAverage = movie.voteAverage,
                    voteCount = movie.voteCount,
                    addedAt = if (movie.addedAt != 0L) movie.addedAt else System.currentTimeMillis()
                )
            }
            if (watchlistEntities.isNotEmpty()) {
                watchlistDao.insertAll(watchlistEntities)
            }

            val ratingEntities = backup.ratings.map { rating ->
                UserRatingEntity(movieId = rating.movieId, rating = rating.rating)
            }
            if (ratingEntities.isNotEmpty()) {
                userRatingDao.insertAll(ratingEntities)
            }

            val memoEntities = backup.memos.map { memo ->
                val now = System.currentTimeMillis()
                MemoEntity(
                    movieId = memo.movieId,
                    content = memo.content,
                    createdAt = if (memo.createdAt != 0L) memo.createdAt else now,
                    updatedAt = if (memo.updatedAt != 0L) memo.updatedAt else now
                )
            }
            if (memoEntities.isNotEmpty()) {
                memoDao.insertAll(memoEntities)
            }

            val tagEntities = backup.tags.map { tag ->
                MovieTagEntity(
                    movieId = tag.movieId,
                    tagName = tag.tagName,
                    addedAt = if (tag.addedAt != 0L) tag.addedAt else System.currentTimeMillis()
                )
            }
            if (tagEntities.isNotEmpty()) {
                tagEntities.forEach { movieTagDao.insertTag(it) }
            }
        }
    }
}
