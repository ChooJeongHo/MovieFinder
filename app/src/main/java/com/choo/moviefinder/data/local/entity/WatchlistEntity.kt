package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.BackupMovie
import com.choo.moviefinder.domain.model.Movie

@Entity(
    tableName = "watchlist_movies",
    indices = [Index(value = ["addedAt"]), Index(value = ["title"]), Index(value = ["voteAverage"])]
)
data class WatchlistEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val addedAt: Long = System.currentTimeMillis(),
    val reminderDate: Long? = null
)

// 워치리스트 Entity를 도메인 Movie 모델로 변환
fun WatchlistEntity.toDomain() = Movie(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

fun WatchlistEntity.toBackupMovie() = BackupMovie(
    id = id, title = title, posterPath = posterPath, backdropPath = backdropPath,
    overview = overview, releaseDate = releaseDate, voteAverage = voteAverage,
    voteCount = voteCount, addedAt = addedAt
)

// 도메인 Movie를 워치리스트 Entity로 변환
fun Movie.toWatchlistEntity() = WatchlistEntity(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)
