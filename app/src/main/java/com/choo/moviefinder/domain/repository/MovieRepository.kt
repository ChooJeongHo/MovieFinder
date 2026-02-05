package com.choo.moviefinder.domain.repository

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    fun getNowPlayingMovies(): Flow<PagingData<Movie>>

    fun getPopularMovies(): Flow<PagingData<Movie>>

    fun searchMovies(query: String, year: Int? = null): Flow<PagingData<Movie>>

    suspend fun getMovieDetail(movieId: Int): MovieDetail

    suspend fun getMovieCredits(movieId: Int): List<Cast>

    suspend fun getSimilarMovies(movieId: Int): List<Movie>

    suspend fun getMovieTrailerKey(movieId: Int): String?

    fun getFavoriteMovies(): Flow<List<Movie>>

    suspend fun toggleFavorite(movie: Movie)

    fun isFavorite(movieId: Int): Flow<Boolean>

    fun getRecentSearches(): Flow<List<String>>

    suspend fun saveSearchQuery(query: String)

    suspend fun deleteSearchQuery(query: String)

    suspend fun clearSearchHistory()
}