package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.RecentSearchDao
import com.choo.moviefinder.data.local.entity.RecentSearchEntity
import com.choo.moviefinder.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchHistoryRepositoryImpl @Inject constructor(
    private val recentSearchDao: RecentSearchDao
) : SearchHistoryRepository {

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
}
