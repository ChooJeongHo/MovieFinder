package com.choo.moviefinder.domain.repository

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface MovieRepository {

    fun getNowPlayingMovies(): Flow<PagingData<Movie>>

    fun getPopularMovies(): Flow<PagingData<Movie>>

    fun searchMovies(query: String, year: Int? = null): Flow<PagingData<Movie>>

    fun discoverMovies(
        genres: Set<Int> = emptySet(),
        sortBy: String = "popularity.desc",
        year: Int? = null
    ): Flow<PagingData<Movie>>

    suspend fun getMovieDetail(movieId: Int): MovieDetail

    suspend fun getMovieCredits(movieId: Int): List<Cast>

    suspend fun getSimilarMovies(movieId: Int): List<Movie>

    suspend fun getMovieTrailerKey(movieId: Int): String?

    suspend fun getMovieReviews(movieId: Int): List<Review>

    suspend fun getMovieCertification(movieId: Int): String?

    suspend fun getGenreList(): List<Genre>

    fun getFavoriteMovies(): Flow<List<Movie>>

    suspend fun toggleFavorite(movie: Movie)

    fun isFavorite(movieId: Int): Flow<Boolean>

    fun getRecentSearches(): Flow<List<String>>

    suspend fun saveSearchQuery(query: String)

    suspend fun deleteSearchQuery(query: String)

    suspend fun clearSearchHistory()

    // Watch History
    fun getWatchHistory(): Flow<List<Movie>>

    suspend fun saveWatchHistory(movie: Movie)

    suspend fun clearWatchHistory()

    // Watchlist
    fun getWatchlistMovies(): Flow<List<Movie>>

    suspend fun toggleWatchlist(movie: Movie)

    fun isInWatchlist(movieId: Int): Flow<Boolean>

    // User Rating
    fun getUserRating(movieId: Int): Flow<Float?>

    suspend fun setUserRating(movieId: Int, rating: Float)

    suspend fun deleteUserRating(movieId: Int)
}