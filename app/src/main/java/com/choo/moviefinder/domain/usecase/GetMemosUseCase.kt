package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Memo
import com.choo.moviefinder.domain.repository.MemoRepository
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Reusable
class GetMemosUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    // 영화의 메모 목록을 실시간 Flow로 반환한다
    operator fun invoke(movieId: Int): Flow<List<Memo>> =
        repository.getMemos(movieId)
}
