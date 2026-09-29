package com.choo.moviefinder.data.repository

import com.choo.moviefinder.core.util.AppLanguageProvider
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.util.safeApiCall
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.PersonDetail
import com.choo.moviefinder.domain.model.PersonSearchItem
import com.choo.moviefinder.domain.repository.PersonRepository
import javax.inject.Inject

class PersonRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService,
    private val appLanguageProvider: AppLanguageProvider
) : PersonRepository {

    // 인물 상세 정보를 API에서 조회 (앱 언어를 따름)
    override suspend fun getPersonDetail(personId: Int): PersonDetail {
        require(personId > 0) { "Person ID must be positive" }
        return safeApiCall {
            apiService.getPersonDetail(personId, appLanguageProvider.currentApiLanguage()).toDomain()
        }
    }

    // 인물 출연 영화 목록을 API에서 조회 (앱 언어를 따름)
    override suspend fun getPersonMovieCredits(personId: Int): List<Movie> {
        require(personId > 0) { "Person ID must be positive" }
        return safeApiCall {
            val response = apiService.getPersonMovieCredits(personId, appLanguageProvider.currentApiLanguage())
            (response.cast + response.crew)
                .distinctBy { it.id }
                .sortedByDescending { it.releaseDate }
                .map { it.toDomain() }
        }
    }

    // 이름으로 배우/인물을 검색하여 결과 목록을 반환한다 (앱 언어를 따름)
    override suspend fun searchPerson(query: String): List<PersonSearchItem> {
        require(query.isNotBlank()) { "Search query must not be blank" }
        return safeApiCall {
            apiService.searchPerson(query, language = appLanguageProvider.currentApiLanguage())
                .results.map { it.toDomain() }
        }
    }
}
