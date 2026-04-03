package com.choo.moviefinder.data.repository

import androidx.room.withTransaction
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.data.local.entity.UserRatingEntity
import com.choo.moviefinder.data.local.entity.WatchlistEntity
import com.choo.moviefinder.domain.model.BackupMemo
import com.choo.moviefinder.domain.model.BackupMovie
import com.choo.moviefinder.domain.model.BackupRating
import com.choo.moviefinder.domain.model.UserDataBackup
import com.choo.moviefinder.domain.repository.BackupRepository
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val database: MovieDatabase,
    private val favoriteMovieDao: FavoriteMovieDao,
    private val watchlistDao: WatchlistDao,
    private val userRatingDao: UserRatingDao,
    private val memoDao: MemoDao
) : BackupRepository {

    // 즐겨찾기, 워치리스트, 평점, 메모를 백업 모델로 내보낸다
    override suspend fun exportUserData(): UserDataBackup {
        val favorites = favoriteMovieDao.getAllFavoritesOnce().map { entity ->
            BackupMovie(
                id = entity.id,
                title = entity.title,
                posterPath = entity.posterPath,
                voteAverage = entity.voteAverage,
                overview = entity.overview
            )
        }
        val watchlist = watchlistDao.getAllWatchlistOnce().map { entity ->
            BackupMovie(
                id = entity.id,
                title = entity.title,
                posterPath = entity.posterPath,
                voteAverage = entity.voteAverage,
                overview = entity.overview
            )
        }
        val ratings = userRatingDao.getAllRatings().map { entity ->
            BackupRating(movieId = entity.movieId, rating = entity.rating)
        }
        val memos = memoDao.getAllMemos().map { entity ->
            BackupMemo(movieId = entity.movieId, content = entity.content)
        }
        return UserDataBackup(
            favorites = favorites,
            watchlist = watchlist,
            ratings = ratings,
            memos = memos
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
                    backdropPath = null,
                    overview = movie.overview,
                    releaseDate = "",
                    voteAverage = movie.voteAverage,
                    voteCount = 0
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
                    backdropPath = null,
                    overview = movie.overview,
                    releaseDate = "",
                    voteAverage = movie.voteAverage,
                    voteCount = 0
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
                MemoEntity(movieId = memo.movieId, content = memo.content)
            }
            if (memoEntities.isNotEmpty()) {
                memoDao.insertAll(memoEntities)
            }
        }
    }
}
