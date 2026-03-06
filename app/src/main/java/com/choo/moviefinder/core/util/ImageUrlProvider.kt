package com.choo.moviefinder.core.util

import com.choo.moviefinder.BuildConfig

object ImageUrlProvider {
    private const val POSTER_SIZE = "w500"
    private const val BACKDROP_SIZE = "w780"
    private const val PROFILE_SIZE = "w185"

    // TMDB 포스터 이미지 전체 URL을 생성한다
    fun posterUrl(path: String?): String? = path?.let {
        "${BuildConfig.TMDB_IMAGE_BASE_URL}$POSTER_SIZE$it"
    }

    // TMDB 배경 이미지 전체 URL을 생성한다
    fun backdropUrl(path: String?): String? = path?.let {
        "${BuildConfig.TMDB_IMAGE_BASE_URL}$BACKDROP_SIZE$it"
    }

    // TMDB 프로필(출연진) 이미지 전체 URL을 생성한다
    fun profileUrl(path: String?): String? = path?.let {
        "${BuildConfig.TMDB_IMAGE_BASE_URL}$PROFILE_SIZE$it"
    }
}