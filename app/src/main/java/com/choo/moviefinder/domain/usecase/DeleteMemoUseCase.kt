package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MemoRepository
import javax.inject.Inject

class DeleteMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    // 메모를 삭제한다
    suspend operator fun invoke(memoId: Long) =
        repository.deleteMemo(memoId)
}
