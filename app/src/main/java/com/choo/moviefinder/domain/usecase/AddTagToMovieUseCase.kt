package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.TagRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class AddTagToMovieUseCase @Inject constructor(
    private val repository: TagRepository
) {
    // 영화에 태그를 추가한다
    suspend operator fun invoke(movieId: Int, tagName: String) =
        repository.addTag(movieId, tagName)
}
