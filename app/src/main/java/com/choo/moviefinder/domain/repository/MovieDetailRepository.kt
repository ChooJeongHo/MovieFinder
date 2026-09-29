package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.CollectionDetail
import com.choo.moviefinder.domain.model.Credits
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review
import com.choo.moviefinder.domain.model.WatchProvider

interface MovieDetailRepository {

    // 영화 ID로 상세 정보를 조회한다
    suspend fun getMovieDetail(movieId: Int): MovieDetail

    // 영화 ID로 출연진 목록을 조회한다
    suspend fun getMovieCredits(movieId: Int): Credits

    // 영화 ID로 비슷한 영화 목록을 조회한다
    suspend fun getSimilarMovies(movieId: Int): List<Movie>

    // 영화 ID로 추천 영화 목록을 조회한다
    suspend fun getMovieRecommendations(movieId: Int): List<Movie>

    // 영화 ID로 YouTube 예고편 키를 조회한다
    suspend fun getMovieTrailerKey(movieId: Int): String?

    // 영화 ID로 콘텐츠 등급 정보를 조회한다
    suspend fun getMovieCertification(movieId: Int): String?

    // 영화 ID로 사용자 리뷰 목록을 조회한다
    suspend fun getMovieReviews(movieId: Int): List<Review>

    // 컬렉션 ID로 컬렉션 상세 정보를 조회한다
    suspend fun getCollection(collectionId: Int): CollectionDetail

    // 영화 ID로 스트리밍 제공 정보를 조회한다 (KR 우선, US 폴백)
    suspend fun getWatchProviders(movieId: Int): List<WatchProvider>

    // movieId로 한국어(ko-KR) 제목을 조회한다 (KMRB 등급 매칭용 - 화면 표시 title이 앱 언어를
    // 따르더라도 이 값은 항상 한국어를 유지해야 KOFIC/KMRB와 매칭 가능)
    suspend fun getKoreanTitle(movieId: Int): String
}
