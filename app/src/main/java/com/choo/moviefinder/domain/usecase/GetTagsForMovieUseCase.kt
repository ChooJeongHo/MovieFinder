package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.MovieTag
import com.choo.moviefinder.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTagsForMovieUseCase @Inject constructor(
    private val repository: TagRepository
) {
    // 특정 영화의 태그 목록을 실시간 Flow로 반환한다
    operator fun invoke(movieId: Int): Flow<List<MovieTag>> =
        repository.getTagsForMovie(movieId)
}
