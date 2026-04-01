package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesByTagUseCase @Inject constructor(
    private val repository: TagRepository
) {
    // 특정 태그가 달린 즐겨찾기 영화 목록을 실시간 Flow로 반환한다
    operator fun invoke(tagName: String): Flow<List<Movie>> =
        repository.getFavoritesByTag(tagName)
}
