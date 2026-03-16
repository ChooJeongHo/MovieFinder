package com.choo.moviefinder.domain.model

data class MovieDetail(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val runtime: Int?,
    val genres: List<Genre>,
    val tagline: String?,
    val budget: Long = 0,
    val revenue: Long = 0,
    val originalLanguage: String = "",
    val imdbId: String? = null,
    val status: String = ""
) {
    // MovieDetail을 Movie 도메인 모델로 변환
    fun toMovie() = Movie(
        id = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        overview = overview,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount
    )
}

data class Genre(
    val id: Int,
    val name: String
)
