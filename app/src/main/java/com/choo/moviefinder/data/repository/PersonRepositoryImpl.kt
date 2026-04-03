package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.PersonDetail
import com.choo.moviefinder.domain.model.PersonSearchItem
import com.choo.moviefinder.domain.repository.PersonRepository
import javax.inject.Inject

class PersonRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService
) : PersonRepository {

    // 인물 상세 정보를 API에서 조회
    override suspend fun getPersonDetail(personId: Int): PersonDetail {
        require(personId > 0) { "Person ID must be positive" }
        return apiService.getPersonDetail(personId).toDomain()
    }

    // 인물 출연 영화 목록을 API에서 조회
    override suspend fun getPersonMovieCredits(personId: Int): List<Movie> {
        require(personId > 0) { "Person ID must be positive" }
        return apiService.getPersonMovieCredits(personId).cast.map { it.toDomain() }
    }

    // 이름으로 배우/인물을 검색하여 결과 목록을 반환한다
    override suspend fun searchPerson(query: String): List<PersonSearchItem> {
        require(query.isNotBlank()) { "Search query must not be blank" }
        return apiService.searchPerson(query).results.map { it.toDomain() }
    }
}
