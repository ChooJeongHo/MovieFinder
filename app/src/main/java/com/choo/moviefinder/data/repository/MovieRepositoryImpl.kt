package com.choo.moviefinder.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.data.local.entity.toEntity
import com.choo.moviefinder.data.local.entity.toWatchHistoryEntity
import com.choo.moviefinder.data.local.entity.toWatchlistEntity
import com.choo.moviefinder.data.paging.DiscoverPagingSource
import com.choo.moviefinder.data.paging.MoviePagingSource
import com.choo.moviefinder.data.paging.MovieRemoteMediator
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.util.Constants
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review
import com.choo.moviefinder.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@Suppress("TooManyFunctions")
class MovieRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService,
    private val database: MovieDatabase,
    private val favoriteMovieDao: FavoriteMovieDao,
    private val recentSearchDao: RecentSearchDao,
    private val cachedMovieDao: CachedMovieDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao
) : MovieRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getNowPlayingMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            remoteMediator = MovieRemoteMediator(
                apiService = apiService,
                database = database,
                cachedMovieDao = cachedMovieDao,
                remoteKeyDao = remoteKeyDao,
                category = MovieRemoteMediator.CATEGORY_NOW_PLAYING
            ),
            pagingSourceFactory = {
                cachedMovieDao.getMoviesByCategory(MovieRemoteMediator.CATEGORY_NOW_PLAYING)
            }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getPopularMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            remoteMediator = MovieRemoteMediator(
                apiService = apiService,
                database = database,
                cachedMovieDao = cachedMovieDao,
                remoteKeyDao = remoteKeyDao,
                category = MovieRemoteMediator.CATEGORY_POPULAR
            ),
            pagingSourceFactory = {
                cachedMovieDao.getMoviesByCategory(MovieRemoteMediator.CATEGORY_POPULAR)
            }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override fun searchMovies(query: String, year: Int?): Flow<PagingData<Movie>> {
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            pagingSourceFactory = {
                MoviePagingSource(apiService, query, year)
            }
        ).flow
    }

    override fun discoverMovies(
        genres: Set<Int>,
        sortBy: String,
        year: Int?
    ): Flow<PagingData<Movie>> {
        val genresParam = if (genres.isNotEmpty()) genres.joinToString(",") else null
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            pagingSourceFactory = {
                DiscoverPagingSource(apiService, genresParam, sortBy, year)
            }
        ).flow
    }

    override suspend fun getMovieDetail(movieId: Int): MovieDetail {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getMovieDetail(movieId).toDomain()
    }

    override suspend fun getMovieCredits(movieId: Int): List<Cast> {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getMovieCredits(movieId).cast
            .sortedBy { it.order }
            .map { it.toDomain() }
    }

    override suspend fun getSimilarMovies(movieId: Int): List<Movie> {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getSimilarMovies(movieId).results.map { it.toDomain() }
    }

    override suspend fun getMovieTrailerKey(movieId: Int): String? {
        require(movieId > 0) { "Movie ID must be positive" }
        val response = apiService.getMovieVideos(movieId)
        return response.results
            .filter { it.site == "YouTube" && it.type == "Trailer" }
            .sortedByDescending { it.official }
            .firstOrNull()?.key
            ?: response.results
                .filter { it.site == "YouTube" }
                .firstOrNull()?.key
    }

    override suspend fun getMovieReviews(movieId: Int): List<Review> {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getMovieReviews(movieId).results.map { it.toDomain() }
    }

    override suspend fun getMovieCertification(movieId: Int): String? {
        require(movieId > 0) { "Movie ID must be positive" }
        val response = apiService.getMovieReleaseDates(movieId)
        // KR 우선, US 폴백
        val krResult = response.results.find { it.iso31661 == "KR" }
        val usResult = response.results.find { it.iso31661 == "US" }
        val result = krResult ?: usResult ?: return null
        return result.releaseDates
            .map { it.certification }
            .firstOrNull { it.isNotBlank() }
    }

    override suspend fun getGenreList(): List<Genre> {
        return apiService.getGenreList().toDomain()
    }

    override fun getFavoriteMovies(): Flow<List<Movie>> {
        return favoriteMovieDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun toggleFavorite(movie: Movie) {
        favoriteMovieDao.toggleFavorite(movie.toEntity())
    }

    override fun isFavorite(movieId: Int): Flow<Boolean> {
        return favoriteMovieDao.isFavorite(movieId)
    }

    override fun getRecentSearches(): Flow<List<String>> {
        return recentSearchDao.getRecentSearches().map { entities ->
            entities.map { it.query }
        }
    }

    override suspend fun saveSearchQuery(query: String) {
        require(query.isNotBlank()) { "Search query must not be blank" }
        recentSearchDao.insert(RecentSearchEntity(query = query.trim()))
    }

    override suspend fun deleteSearchQuery(query: String) {
        recentSearchDao.delete(query)
    }

    override suspend fun clearSearchHistory() {
        recentSearchDao.clearAll()
    }

    // Watch History
    override fun getWatchHistory(): Flow<List<Movie>> {
        return watchHistoryDao.getRecentHistory(Constants.WATCH_HISTORY_LIMIT).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveWatchHistory(movie: Movie) {
        watchHistoryDao.insert(movie.toWatchHistoryEntity())
    }

    override suspend fun clearWatchHistory() {
        watchHistoryDao.clearAll()
    }

    // Watchlist
    override fun getWatchlistMovies(): Flow<List<Movie>> {
        return watchlistDao.getAllWatchlist().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun toggleWatchlist(movie: Movie) {
        watchlistDao.toggleWatchlist(movie.toWatchlistEntity())
    }

    override fun isInWatchlist(movieId: Int): Flow<Boolean> {
        return watchlistDao.isInWatchlist(movieId)
    }
}
