package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.Movie
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "watch_history",
    indices = [
        Index(value = ["movieId"]),
        Index(value = ["watchedAt"]),
        Index(value = ["yearMonth"])
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val movieId: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val watchedAt: Long = System.currentTimeMillis(),
    val yearMonth: String = "",
    val genres: String = ""
)

// 시청 기록 Entity를 도메인 Movie 모델로 변환
fun WatchHistoryEntity.toDomain() = Movie(
    id = movieId,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

// 도메인 Movie를 시청 기록 Entity로 변환
fun Movie.toWatchHistoryEntity(genres: String = ""): WatchHistoryEntity {
    val now = System.currentTimeMillis()
    return WatchHistoryEntity(
        rowId = 0,
        movieId = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        overview = overview,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        watchedAt = now,
        yearMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(now)),
        genres = genres
    )
}
