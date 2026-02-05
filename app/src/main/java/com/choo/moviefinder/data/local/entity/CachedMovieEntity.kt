package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.choo.moviefinder.domain.model.Movie

@Entity(
    tableName = "cached_movies",
    primaryKeys = ["id", "category"],
    indices = [Index(value = ["category"])]
)
data class CachedMovieEntity(
    val id: Int,
    val category: String,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val page: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

fun CachedMovieEntity.toDomain() = Movie(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

fun Movie.toCachedEntity(category: String, page: Int) = CachedMovieEntity(
    id = id,
    category = category,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    page = page
)
