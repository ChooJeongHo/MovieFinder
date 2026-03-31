package com.choo.moviefinder.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {

    // 최근 검색어 목록을 Flow로 반환한다
    fun getRecentSearches(): Flow<List<String>>

    // 검색어를 최근 검색 기록에 저장한다
    suspend fun saveSearchQuery(query: String)

    // 특정 검색어를 최근 검색 기록에서 삭제한다
    suspend fun deleteSearchQuery(query: String)

    // 모든 최근 검색 기록을 삭제한다
    suspend fun clearSearchHistory()
}
