package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.BackupMovie
import com.choo.moviefinder.domain.model.Movie

@Entity(
    tableName = "favorite_movies",
    indices = [Index(value = ["addedAt"]), Index(value = ["title"]), Index(value = ["voteAverage"])]
)
data class FavoriteMovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val addedAt: Long = System.currentTimeMillis()
)

// 즐겨찾기 Entity를 도메인 Movie 모델로 변환
fun FavoriteMovieEntity.toDomain() = Movie(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

fun FavoriteMovieEntity.toBackupMovie() = BackupMovie(
    id = id, title = title, posterPath = posterPath, backdropPath = backdropPath,
    overview = overview, releaseDate = releaseDate, voteAverage = voteAverage,
    voteCount = voteCount, addedAt = addedAt
)

// 도메인 Movie를 즐겨찾기 Entity로 변환
fun Movie.toEntity() = FavoriteMovieEntity(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)