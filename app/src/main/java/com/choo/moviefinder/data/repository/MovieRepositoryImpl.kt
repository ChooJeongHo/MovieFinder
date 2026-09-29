package com.choo.moviefinder.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.choo.moviefinder.core.util.AppLanguageProvider
import com.choo.moviefinder.core.util.NetworkMonitor
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.MovieKoreanTitleCacheDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.entity.MovieKoreanTitleCacheEntity
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.data.paging.DiscoverPagingSource
import com.choo.moviefinder.data.paging.MoviePagingSource
import com.choo.moviefinder.data.paging.MovieRemoteMediator
import com.choo.moviefinder.data.paging.TrendingPagingSource
import com.choo.moviefinder.data.paging.UpcomingPagingSource
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.util.Constants
import com.choo.moviefinder.data.util.safeApiCall
import com.choo.moviefinder.domain.model.CollectionDetail
import com.choo.moviefinder.domain.model.Credits
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review
import com.choo.moviefinder.domain.model.WatchProvider
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
    private val movieKoreanTitleCacheDao: MovieKoreanTitleCacheDao,
    private val networkMonitor: NetworkMonitor,
    private val appLanguageProvider: AppLanguageProvider
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
                networkMonitor = networkMonitor,
                appLanguageProvider = appLanguageProvider
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
                TrendingPagingSource(apiService, appLanguageProvider)
            }
        ).flow
    }

    // 개봉 예정 영화를 네트워크 페이징 조회
    override fun getUpcomingMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            pagingSourceFactory = {
                UpcomingPagingSource(apiService, appLanguageProvider)
            }
        ).flow
    }

    // 검색어와 연도 필터로 영화를 네트워크 페이징 검색 (ko-KR 고정 - KMRB 필터 성능 문제로 되돌림)
    override fun searchMovies(query: String, year: Int?): Flow<PagingData<Movie>> {
        require(query.isNotBlank()) { "Search query must not be blank" }
        return Pager(
            config = Constants.DEFAULT_PAGING_CONFIG,
            pagingSourceFactory = {
                MoviePagingSource(apiService, query, year)
            }
        ).flow
    }

    // 검색어로 영화를 1페이지만 즉시 조회한다 (박스오피스-TMDB 매칭 등 단발성 조회용)
    // 앱 언어와 무관하게 항상 ko-KR로 조회한다 — KOFIC 영화명(한국어)과의 매칭 정확도 유지를 위함.
    override suspend fun searchMoviesOnce(query: String): List<Movie> {
        require(query.isNotBlank()) { "Search query must not be blank" }
        return safeApiCall {
            apiService.searchMovies(query = query, page = 1, language = Constants.LANGUAGE_KO)
                .results.map { it.toDomain() }
        }
    }

    // 로컬에 캐싱된 현재 상영작/인기 영화 전체를 네트워크 호출 없이 스냅샷으로 조회
    override suspend fun getCachedMoviesSnapshot(): List<Movie> =
        cachedMovieDao.getAllByCategories(
            listOf(MovieRemoteMediator.CATEGORY_NOW_PLAYING, MovieRemoteMediator.CATEGORY_POPULAR)
        ).map { it.toDomain() }

    // 앱 언어 변경 시 현재 상영작/인기 영화 로컬 캐시를 무효화한다 (이전 언어 데이터 잔존 방지)
    override suspend fun invalidateHomeMovieCache() {
        database.withTransaction {
            listOf(MovieRemoteMediator.CATEGORY_NOW_PLAYING, MovieRemoteMediator.CATEGORY_POPULAR).forEach {
                cachedMovieDao.clearByCategory(it)
                remoteKeyDao.clearByCategory(it)
            }
        }
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
                DiscoverPagingSource(apiService, genresParam, sortBy, year, appLanguageProvider)
            }
        ).flow
    }

    // 영화 상세 정보를 API에서 조회 (앱 언어를 따름)
    override suspend fun getMovieDetail(movieId: Int): MovieDetail {
        require(movieId > 0) { "Movie ID must be positive" }
        return safeApiCall { apiService.getMovieDetail(movieId, appLanguageProvider.currentApiLanguage()).toDomain() }
    }

    // 영화 출연진 및 감독 정보를 조회 (cast는 order 기준 정렬, 앱 언어를 따름)
    override suspend fun getMovieCredits(movieId: Int): Credits {
        require(movieId > 0) { "Movie ID must be positive" }
        return safeApiCall {
            val response = apiService.getMovieCredits(movieId, appLanguageProvider.currentApiLanguage())
            Credits(
                cast = response.cast.sortedBy { it.order }.map { it.toDomain() },
                directors = response.crew.filter { it.job == Constants.CREW_JOB_DIRECTOR }.map { it.name }
            )
        }
    }

    // 비슷한 영화 목록을 API에서 조회 (앱 언어를 따름)
    override suspend fun getSimilarMovies(movieId: Int): List<Movie> {
        require(movieId > 0) { "Movie ID must be positive" }
        return safeApiCall {
            apiService.getSimilarMovies(movieId, language = appLanguageProvider.currentApiLanguage())
                .results.map { it.toDomain() }
        }
    }

    // movieId로 한국어(ko-KR) 제목을 조회한다 (KMRB 등급 매칭용). movieId는 바뀌지 않는 식별자이고
    // 제목도 사실상 불변이라 TTL 없이 영구 캐시한다 — 등급 필터가 켜질 때마다 반복 조회되는 N+1을 피함.
    override suspend fun getKoreanTitle(movieId: Int): String {
        require(movieId > 0) { "Movie ID must be positive" }
        movieKoreanTitleCacheDao.find(movieId)?.let { return it.koreanTitle }
        val koreanTitle = safeApiCall { apiService.getMovieDetail(movieId, Constants.LANGUAGE_KO).title }
        movieKoreanTitleCacheDao.upsert(
            MovieKoreanTitleCacheEntity(movieId, koreanTitle, System.currentTimeMillis())
        )
        return koreanTitle
    }

    // YouTube 예고편 키 조회 (공식 Trailer 우선, YouTube 영상 폴백)
    override suspend fun getMovieTrailerKey(movieId: Int): String? {
        require(movieId > 0) { "Movie ID must be positive" }
        return safeApiCall {
            val youtubeVideos = apiService.getMovieVideos(movieId).results
                .filter { it.site == Constants.VIDEO_SITE_YOUTUBE }
            youtubeVideos
                .filter { it.type == Constants.VIDEO_TYPE_TRAILER }
                .sortedByDescending { it.official }
                .firstOrNull()?.key
                ?: youtubeVideos.firstOrNull()?.key
        }
    }

    // 영화 리뷰 목록을 API에서 조회
    override suspend fun getMovieReviews(movieId: Int): List<Review> {
        require(movieId > 0) { "Movie ID must be positive" }
        return safeApiCall { apiService.getMovieReviews(movieId).results.map { it.toDomain() } }
    }

    // 영화 콘텐츠 등급 조회 (KR 우선, US 폴백)
    override suspend fun getMovieCertification(movieId: Int): String? {
        require(movieId > 0) { "Movie ID must be positive" }
        return safeApiCall {
            val response = apiService.getMovieReleaseDates(movieId)
            val krResult = response.results.find { it.iso31661 == Constants.REGION_KR }
            val usResult = response.results.find { it.iso31661 == Constants.REGION_US }
            val result = krResult ?: usResult ?: return@safeApiCall null
            result.releaseDates
                .map { it.certification }
                .firstOrNull { it.isNotBlank() }
        }
    }

    // 영화 장르 목록을 API에서 조회 (앱 언어를 따름)
    override suspend fun getGenreList(): List<Genre> =
        safeApiCall { apiService.getGenreList(appLanguageProvider.currentApiLanguage()).toDomain() }

    // 추천 영화 목록을 API에서 조회 (앱 언어를 따름)
    override suspend fun getMovieRecommendations(movieId: Int): List<Movie> {
        require(movieId > 0) { "Movie ID must be positive" }
        return safeApiCall {
            apiService.getMovieRecommendations(movieId, language = appLanguageProvider.currentApiLanguage())
                .results.map { it.toDomain() }
        }
    }

    // 컬렉션 상세 정보를 API에서 조회 (앱 언어를 따름)
    override suspend fun getCollection(collectionId: Int): CollectionDetail =
        safeApiCall { apiService.getCollection(collectionId, appLanguageProvider.currentApiLanguage()).toDomain() }

    // 스트리밍 제공 정보를 API에서 조회 (KR 우선, US 폴백, flatrate → rent → buy 순)
    override suspend fun getWatchProviders(movieId: Int): List<WatchProvider> {
        require(movieId > 0) { "Movie ID must be positive" }
        return safeApiCall {
            val response = apiService.getWatchProviders(movieId)
            val regionResult = response.results[Constants.REGION_KR] ?: response.results[Constants.REGION_US]
            val providers = regionResult?.flatrate?.ifEmpty { regionResult.rent }?.ifEmpty { regionResult.buy }
            providers?.map { WatchProvider(it.providerId, it.providerName, it.logoPath) } ?: emptyList()
        }
    }
}
