package com.choo.moviefinder.domain.repository

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Memo
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.PersonSearchItem
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.DailyWatchCount
import com.choo.moviefinder.domain.model.MonthlyWatchCount
import com.choo.moviefinder.domain.model.RatingBucket
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.PersonDetail
import com.choo.moviefinder.domain.model.Review
import com.choo.moviefinder.domain.model.UserDataBackup
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface MovieRepository {

    // 현재 상영 중인 영화 목록을 페이징 데이터로 반환한다
    fun getNowPlayingMovies(): Flow<PagingData<Movie>>

    // 인기 영화 목록을 페이징 데이터로 반환한다
    fun getPopularMovies(): Flow<PagingData<Movie>>

    // 검색어와 연도 필터로 영화를 검색하여 페이징 데이터로 반환한다
    fun searchMovies(query: String, year: Int? = null): Flow<PagingData<Movie>>

    // 일별 트렌딩 영화 목록을 페이징 데이터로 반환한다
    fun getTrendingMovies(): Flow<PagingData<Movie>>

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

    // 영화를 장르 정보와 함께 시청 기록에 저장한다
    suspend fun saveWatchHistoryWithGenres(movie: Movie, genres: String)

    // 총 시청 편수를 Flow로 반환한다
    fun getTotalWatchedCount(): Flow<Int>

    // 특정 시점 이후 시청 편수를 Flow로 반환한다
    fun getWatchedCountSince(since: Long): Flow<Int>

    // 모든 시청 기록의 장르 문자열 목록을 Flow로 반환한다
    fun getAllWatchedGenres(): Flow<List<String>>

    // 모든 사용자 평점의 평균을 Flow로 반환한다
    fun getAverageUserRating(): Flow<Float?>

    // 월별 시청 편수를 Flow로 반환한다
    fun getMonthlyWatchCounts(): Flow<List<MonthlyWatchCount>>

    // 평점별 개수 분포를 Flow로 반환한다
    fun getRatingDistribution(): Flow<List<RatingBucket>>

    // 일별 시청 편수를 Flow로 반환한다
    fun getDailyWatchCounts(): Flow<List<DailyWatchCount>>

    // 영화의 메모 목록을 Flow로 반환한다
    fun getMemos(movieId: Int): Flow<List<Memo>>

    // 영화에 메모를 저장한다
    suspend fun saveMemo(movieId: Int, content: String)

    // 메모 내용을 수정한다
    suspend fun updateMemo(memoId: Long, content: String)

    // 메모를 삭제한다
    suspend fun deleteMemo(memoId: Long)

    // 영화 ID로 추천 영화 목록을 조회한다
    suspend fun getMovieRecommendations(movieId: Int): List<Movie>

    // 인물 ID로 상세 정보를 조회한다
    suspend fun getPersonDetail(personId: Int): PersonDetail

    // 인물 ID로 출연 영화 목록을 조회한다
    suspend fun getPersonMovieCredits(personId: Int): List<Movie>

    // 사용자 데이터(즐겨찾기, 워치리스트, 평점, 메모)를 백업 모델로 내보낸다
    suspend fun exportUserData(): UserDataBackup

    // 백업 모델에서 사용자 데이터를 가져와 기존 데이터와 병합한다
    suspend fun importUserData(backup: UserDataBackup)

    // 이름으로 배우/인물을 검색하여 결과 목록을 반환한다
    suspend fun searchPerson(query: String): List<PersonSearchItem>
}