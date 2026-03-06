package com.choo.moviefinder.presentation.favorite

import com.choo.moviefinder.domain.model.Movie

enum class FavoriteSortOrder {
    ADDED_DATE,
    TITLE,
    RATING;

    // 정렬 옵션에 따라 영화 목록 정렬 (추가일/제목/평점)
    fun apply(movies: List<Movie>): List<Movie> = when (this) {
        ADDED_DATE -> movies
        TITLE -> movies.sortedBy { it.title }
        RATING -> movies.sortedByDescending { it.voteAverage }
    }
}
