package com.choo.moviefinder.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import com.choo.moviefinder.core.util.NetworkMonitor
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.data.paging.DiscoverPagingSource
import com.choo.moviefinder.data.paging.MoviePagingSource
import com.choo.moviefinder.data.paging.MovieRemoteMediator
import com.choo.moviefinder.data.paging.TrendingPagingSource
import com.choo.moviefinder.data.paging.UpcomingPagingSource
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.util.Constants
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.CollectionDetail
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
    private val cachedMovieDao: CachedMovieDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val networkMonitor: NetworkMonitor
) : MovieRepository {

    // RemoteMediator 기반 오프라인 캐시 페이징 조회 (카테고리별 공통 헬퍼)
    @OptIn(ExperimentalPagingApi::class)
    private fun getCachedMovies(category: String): Flow<PagingData<Movie>> {
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            remoteMediator = MovieRemoteMediator(
                apiService = apiService,
                database = database,
                cachedMovieDao = cachedMovieDao,
                remoteKeyDao = remoteKeyDao,
                category = category,
                networkMonitor = networkMonitor
            ),
            pagingSourceFactory = {
                cachedMovieDao.getMoviesByCategory(category)
            }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    // 현재 상영작을 RemoteMediator 기반 오프라인 캐시로 페이징 조회
    override fun getNowPlayingMovies(): Flow<PagingData<Movie>> =
        getCachedMovies(MovieRemoteMediator.CATEGORY_NOW_PLAYING)

    // 인기 영화를 RemoteMediator 기반 오프라인 캐시로 페이징 조회
    override fun getPopularMovies(): Flow<PagingData<Movie>> =
        getCachedMovies(MovieRemoteMediator.CATEGORY_POPULAR)

    // 일별 트렌딩 영화를 네트워크 페이징 조회
    override fun getTrendingMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            pagingSourceFactory = {
                TrendingPagingSource(apiService)
            }
        ).flow
    }

    // 개봉 예정 영화를 네트워크 페이징 조회
    override fun getUpcomingMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            pagingSourceFactory = {
                UpcomingPagingSource(apiService)
            }
        ).flow
    }

    // 검색어와 연도 필터로 영화를 네트워크 페이징 검색
    override fun searchMovies(query: String, year: Int?): Flow<PagingData<Movie>> {
        require(query.isNotBlank()) { "Search query must not be blank" }
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            pagingSourceFactory = {
                MoviePagingSource(apiService, query, year)
            }
        ).flow
    }

    // 장르와 정렬 기준으로 영화를 탐색 (Discover API)
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

    // 영화 상세 정보를 API에서 조회
    override suspend fun getMovieDetail(movieId: Int): MovieDetail {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getMovieDetail(movieId).toDomain()
    }

    // 영화 출연진 정보를 order 기준 정렬하여 조회
    override suspend fun getMovieCredits(movieId: Int): List<Cast> {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getMovieCredits(movieId).cast
            .sortedBy { it.order }
            .map { it.toDomain() }
    }

    // 비슷한 영화 목록을 API에서 조회
    override suspend fun getSimilarMovies(movieId: Int): List<Movie> {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getSimilarMovies(movieId).results.map { it.toDomain() }
    }

    // YouTube 예고편 키 조회 (공식 Trailer 우선, YouTube 영상 폴백)
    override suspend fun getMovieTrailerKey(movieId: Int): String? {
        require(movieId > 0) { "Movie ID must be positive" }
        val youtubeVideos = apiService.getMovieVideos(movieId).results.filter { it.site == "YouTube" }
        return youtubeVideos
            .filter { it.type == "Trailer" }
            .sortedByDescending { it.official }
            .firstOrNull()?.key
            ?: youtubeVideos.firstOrNull()?.key
    }

    // 영화 리뷰 목록을 API에서 조회
    override suspend fun getMovieReviews(movieId: Int): List<Review> {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getMovieReviews(movieId).results.map { it.toDomain() }
    }

    // 영화 콘텐츠 등급 조회 (KR 우선, US 폴백)
    override suspend fun getMovieCertification(movieId: Int): String? {
        require(movieId > 0) { "Movie ID must be positive" }
        val response = apiService.getMovieReleaseDates(movieId)
        val krResult = response.results.find { it.iso31661 == "KR" }
        val usResult = response.results.find { it.iso31661 == "US" }
        val result = krResult ?: usResult ?: return null
        return result.releaseDates
            .map { it.certification }
            .firstOrNull { it.isNotBlank() }
    }

    // 영화 장르 목록을 API에서 조회
    override suspend fun getGenreList(): List<Genre> {
        return apiService.getGenreList().toDomain()
    }

    // 추천 영화 목록을 API에서 조회
    override suspend fun getMovieRecommendations(movieId: Int): List<Movie> {
        require(movieId > 0) { "Movie ID must be positive" }
        return apiService.getMovieRecommendations(movieId).results.map { it.toDomain() }
    }

    // 컬렉션 상세 정보를 API에서 조회
    override suspend fun getCollection(collectionId: Int): CollectionDetail =
        apiService.getCollection(collectionId).toDomain()
}
