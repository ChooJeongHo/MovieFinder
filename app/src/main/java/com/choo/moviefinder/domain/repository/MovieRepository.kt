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

    // 현재 상영 중인 영화 목록을 페이징 데이터로 반환한다
    fun getNowPlayingMovies(): Flow<PagingData<Movie>>

    // 인기 영화 목록을 페이징 데이터로 반환한다
    fun getPopularMovies(): Flow<PagingData<Movie>>

    // 검색어와 연도 필터로 영화를 검색하여 페이징 데이터로 반환한다
    fun searchMovies(query: String, year: Int? = null): Flow<PagingData<Movie>>

    // 장르, 정렬, 연도 필터로 영화를 탐색하여 페이징 데이터로 반환한다
    fun discoverMovies(
        genres: Set<Int> = emptySet(),
        sortBy: String = "popularity.desc",
        year: Int? = null
    ): Flow<PagingData<Movie>>

    // 영화 ID로 상세 정보를 조회한다
    suspend fun getMovieDetail(movieId: Int): MovieDetail

    // 영화 ID로 출연진 목록을 조회한다
    suspend fun getMovieCredits(movieId: Int): List<Cast>

    // 영화 ID로 비슷한 영화 목록을 조회한다
    suspend fun getSimilarMovies(movieId: Int): List<Movie>

    // 영화 ID로 YouTube 예고편 키를 조회한다
    suspend fun getMovieTrailerKey(movieId: Int): String?

    // 영화 ID로 사용자 리뷰 목록을 조회한다
    suspend fun getMovieReviews(movieId: Int): List<Review>

    // 영화 ID로 콘텐츠 등급 정보를 조회한다
    suspend fun getMovieCertification(movieId: Int): String?

    // 영화 장르 목록을 조회한다
    suspend fun getGenreList(): List<Genre>

    // 즐겨찾기한 영화 목록을 Flow로 반환한다
    fun getFavoriteMovies(): Flow<List<Movie>>

    // 영화의 즐겨찾기 상태를 토글한다 (추가/삭제)
    suspend fun toggleFavorite(movie: Movie)

    // 영화 ID로 즐겨찾기 여부를 Flow로 반환한다
    fun isFavorite(movieId: Int): Flow<Boolean>

    // 최근 검색어 목록을 Flow로 반환한다
    fun getRecentSearches(): Flow<List<String>>

    // 검색어를 최근 검색 기록에 저장한다
    suspend fun saveSearchQuery(query: String)

    // 특정 검색어를 최근 검색 기록에서 삭제한다
    suspend fun deleteSearchQuery(query: String)

    // 모든 최근 검색 기록을 삭제한다
    suspend fun clearSearchHistory()

    // 시청 기록 목록을 Flow로 반환한다
    fun getWatchHistory(): Flow<List<Movie>>

    // 영화를 시청 기록에 저장한다
    suspend fun saveWatchHistory(movie: Movie)

    // 모든 시청 기록을 삭제한다
    suspend fun clearWatchHistory()

    // 워치리스트 영화 목록을 Flow로 반환한다
    fun getWatchlistMovies(): Flow<List<Movie>>

    // 영화의 워치리스트 상태를 토글한다 (추가/삭제)
    suspend fun toggleWatchlist(movie: Movie)

    // 영화 ID로 워치리스트 포함 여부를 Flow로 반환한다
    fun isInWatchlist(movieId: Int): Flow<Boolean>

    // 영화 ID로 사용자가 매긴 평점을 Flow로 반환한다
    fun getUserRating(movieId: Int): Flow<Float?>

    // 영화에 사용자 평점을 설정하여 저장한다
    suspend fun setUserRating(movieId: Int, rating: Float)

    // 영화에 매긴 사용자 평점을 삭제한다
    suspend fun deleteUserRating(movieId: Int)
}