package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class SaveMemoUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화에 메모를 저장한다
    suspend operator fun invoke(movieId: Int, content: String) =
        repository.saveMemo(movieId, content)
}
