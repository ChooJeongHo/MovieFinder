package com.choo.moviefinder.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.FavoriteMovieDao
import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.dao.UserRatingDao
import com.choo.moviefinder.data.local.dao.WatchHistoryDao
import com.choo.moviefinder.data.local.dao.WatchlistDao
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
import com.choo.moviefinder.data.local.entity.UserRatingEntity
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
import com.choo.moviefinder.domain.model.Memo
import com.choo.moviefinder.domain.model.MonthlyWatchCount
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
    private val watchlistDao: WatchlistDao,
    private val userRatingDao: UserRatingDao,
    private val memoDao: MemoDao
) : MovieRepository {

    // 현재 상영작을 RemoteMediator 기반 오프라인 캐시로 페이징 조회
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

    // 인기 영화를 RemoteMediator 기반 오프라인 캐시로 페이징 조회
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

    // 검색어와 연도 필터로 영화를 네트워크 페이징 검색
    override fun searchMovies(query: String, year: Int?): Flow<PagingData<Movie>> {
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
        val response = apiService.getMovieVideos(movieId)
        return response.results
            .filter { it.site == "YouTube" && it.type == "Trailer" }
            .sortedByDescending { it.official }
            .firstOrNull()?.key
            ?: response.results
                .filter { it.site == "YouTube" }
                .firstOrNull()?.key
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
        // KR 우선, US 폴백
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

    // 즐겨찾기 영화 목록을 실시간 Flow로 조회
    override fun getFavoriteMovies(): Flow<List<Movie>> {
        return favoriteMovieDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 즐겨찾기 상태 토글 (추가/삭제)
    override suspend fun toggleFavorite(movie: Movie) {
        favoriteMovieDao.toggleFavorite(movie.toEntity())
    }

    // 해당 영화의 즐겨찾기 여부를 실시간 관찰
    override fun isFavorite(movieId: Int): Flow<Boolean> {
        return favoriteMovieDao.isFavorite(movieId)
    }

    // 최근 검색어 목록을 문자열 리스트로 조회
    override fun getRecentSearches(): Flow<List<String>> {
        return recentSearchDao.getRecentSearches().map { entities ->
            entities.map { it.query }
        }
    }

    // 검색어를 trim 후 DB에 저장
    override suspend fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        require(trimmed.isNotBlank()) { "Search query must not be blank" }
        recentSearchDao.insert(RecentSearchEntity(query = trimmed))
    }

    // 특정 검색어 삭제
    override suspend fun deleteSearchQuery(query: String) {
        recentSearchDao.delete(query)
    }

    // 모든 검색 기록 삭제
    override suspend fun clearSearchHistory() {
        recentSearchDao.clearAll()
    }

    // 최근 시청 기록을 도메인 모델로 변환하여 조회
    override fun getWatchHistory(): Flow<List<Movie>> {
        return watchHistoryDao.getRecentHistory(Constants.WATCH_HISTORY_LIMIT).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 영화를 시청 기록에 저장
    override suspend fun saveWatchHistory(movie: Movie) {
        watchHistoryDao.insert(movie.toWatchHistoryEntity())
    }

    // 모든 시청 기록 삭제
    override suspend fun clearWatchHistory() {
        watchHistoryDao.clearAll()
    }

    // 워치리스트 영화 목록을 실시간 Flow로 조회
    override fun getWatchlistMovies(): Flow<List<Movie>> {
        return watchlistDao.getAllWatchlist().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 워치리스트 상태 토글 (추가/삭제)
    override suspend fun toggleWatchlist(movie: Movie) {
        watchlistDao.toggleWatchlist(movie.toWatchlistEntity())
    }

    // 해당 영화의 워치리스트 여부를 실시간 관찰
    override fun isInWatchlist(movieId: Int): Flow<Boolean> {
        return watchlistDao.isInWatchlist(movieId)
    }

    // 영화의 사용자 평점을 실시간 Flow로 조회
    override fun getUserRating(movieId: Int): Flow<Float?> {
        return userRatingDao.getRating(movieId)
    }

    // 사용자 영화 평점 저장 (0.5~5.0 범위 검증)
    override suspend fun setUserRating(movieId: Int, rating: Float) {
        require(movieId > 0) { "Movie ID must be positive" }
        require(rating in 0.5f..5.0f) { "Rating must be between 0.5 and 5.0" }
        userRatingDao.insertRating(UserRatingEntity(movieId = movieId, rating = rating))
    }

    // 사용자 영화 평점 삭제
    override suspend fun deleteUserRating(movieId: Int) {
        userRatingDao.deleteRating(movieId)
    }

    // 영화를 장르 정보와 함께 시청 기록에 저장
    override suspend fun saveWatchHistoryWithGenres(movie: Movie, genres: String) {
        watchHistoryDao.insert(movie.toWatchHistoryEntity(genres))
    }

    // 총 시청 편수를 실시간 Flow로 조회
    override fun getTotalWatchedCount(): Flow<Int> {
        return watchHistoryDao.getTotalCount()
    }

    // 특정 시점 이후 시청 편수를 실시간 Flow로 조회
    override fun getWatchedCountSince(since: Long): Flow<Int> {
        return watchHistoryDao.getCountSince(since)
    }

    // 모든 시청 기록의 장르 문자열 목록을 실시간 Flow로 조회
    override fun getAllWatchedGenres(): Flow<List<String>> {
        return watchHistoryDao.getAllGenres()
    }

    // 모든 사용자 평점의 평균을 실시간 Flow로 조회
    override fun getAverageUserRating(): Flow<Float?> {
        return userRatingDao.getAverageRating()
    }

    // 월별 시청 편수를 도메인 모델로 변환하여 조회
    override fun getMonthlyWatchCounts(): Flow<List<MonthlyWatchCount>> {
        return watchHistoryDao.getMonthlyWatchCounts().map { counts ->
            counts.map { MonthlyWatchCount(yearMonth = it.yearMonth, count = it.count) }
        }
    }

    // 영화의 메모 목록을 최신순으로 실시간 Flow로 조회
    override fun getMemos(movieId: Int): Flow<List<Memo>> {
        require(movieId > 0) { "Movie ID must be positive" }
        return memoDao.getMemosByMovieId(movieId).map { entities ->
            entities.map {
                Memo(
                    id = it.id,
                    movieId = it.movieId,
                    content = it.content,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    // 새 메모를 저장
    override suspend fun saveMemo(movieId: Int, content: String) {
        require(movieId > 0) { "Movie ID must be positive" }
        require(content.isNotBlank()) { "Memo content must not be blank" }
        memoDao.insert(MemoEntity(movieId = movieId, content = content))
    }

    // 기존 메모 내용을 수정
    override suspend fun updateMemo(memoId: Long, content: String) {
        require(content.isNotBlank()) { "Memo content must not be blank" }
        memoDao.updateMemo(memoId = memoId, content = content, updatedAt = System.currentTimeMillis())
    }

    // 메모를 삭제
    override suspend fun deleteMemo(memoId: Long) {
        memoDao.deleteMemo(memoId)
    }
}
