package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Memo
import com.choo.moviefinder.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMemosUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화의 메모 목록을 실시간 Flow로 반환한다
    operator fun invoke(movieId: Int): Flow<List<Memo>> =
        repository.getMemos(movieId)
}
