package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.PersonDetail
import com.choo.moviefinder.domain.model.PersonSearchItem

interface PersonRepository {

    // 인물 ID로 상세 정보를 조회한다
    suspend fun getPersonDetail(personId: Int): PersonDetail

    // 인물 ID로 출연 영화 목록을 조회한다
    suspend fun getPersonMovieCredits(personId: Int): List<Movie>

    // 이름으로 배우/인물을 검색하여 결과 목록을 반환한다
    suspend fun searchPerson(query: String): List<PersonSearchItem>
}
