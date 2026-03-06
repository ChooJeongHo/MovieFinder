package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.Movie

@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["watchedAt"])]
)
data class WatchHistoryEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val watchedAt: Long = System.currentTimeMillis()
)

// 시청 기록 Entity를 도메인 Movie 모델로 변환
fun WatchHistoryEntity.toDomain() = Movie(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

// 도메인 Movie를 시청 기록 Entity로 변환
fun Movie.toWatchHistoryEntity() = WatchHistoryEntity(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)
