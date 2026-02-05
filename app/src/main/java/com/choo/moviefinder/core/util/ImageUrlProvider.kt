package com.choo.moviefinder.core.util

import com.choo.moviefinder.BuildConfig

object ImageUrlProvider {
    private const val POSTER_SIZE = "w500"
    private const val BACKDROP_SIZE = "w780"
    private const val PROFILE_SIZE = "w185"

    fun posterUrl(path: String?): String? = path?.let {
        "${BuildConfig.TMDB_IMAGE_BASE_URL}$POSTER_SIZE$it"
    }

    fun backdropUrl(path: String?): String? = path?.let {
        "${BuildConfig.TMDB_IMAGE_BASE_URL}$BACKDROP_SIZE$it"
    }

    fun profileUrl(path: String?): String? = path?.let {
        "${BuildConfig.TMDB_IMAGE_BASE_URL}$PROFILE_SIZE$it"
    }
}