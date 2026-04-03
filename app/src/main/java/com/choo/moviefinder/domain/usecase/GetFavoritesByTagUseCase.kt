package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesByTagUseCase @Inject constructor(
    private val repository: TagRepository
) {
    // 특정 태그가 달린 즐겨찾기 영화 목록을 실시간 Flow로 반환한다 (기본: 추가일 역순)
    operator fun invoke(tagName: String): Flow<List<Movie>> =
        repository.getFavoritesByTag(tagName)

    // 정렬 순서를 지정하여 태그 필터링 즐겨찾기 목록을 DB ORDER BY로 반환한다
    operator fun invoke(tagName: String, sortOrder: FavoriteSortOrder): Flow<List<Movie>> =
        repository.getFavoritesByTagSorted(tagName, sortOrder)
}
