package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieTag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getTagsForMovie(movieId: Int): Flow<List<MovieTag>>
    fun getAllTagNames(): Flow<List<String>>
    suspend fun addTag(movieId: Int, tagName: String)
    suspend fun removeTag(movieId: Int, tagName: String)
    fun getFavoritesByTag(tagName: String): Flow<List<Movie>>
}
