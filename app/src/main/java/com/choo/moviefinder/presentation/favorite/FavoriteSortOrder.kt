package com.choo.moviefinder.presentation.favorite

import com.choo.moviefinder.domain.model.Movie

enum class FavoriteSortOrder {
    ADDED_DATE,
    TITLE,
    RATING;

    fun apply(movies: List<Movie>): List<Movie> = when (this) {
        ADDED_DATE -> movies
        TITLE -> movies.sortedBy { it.title }
        RATING -> movies.sortedByDescending { it.voteAverage }
    }
}
