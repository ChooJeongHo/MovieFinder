package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.TagRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class RemoveTagFromMovieUseCase @Inject constructor(
    private val repository: TagRepository
) {
    // 영화에서 특정 태그를 제거한다
    suspend operator fun invoke(movieId: Int, tagName: String) =
        repository.removeTag(movieId, tagName)
}
