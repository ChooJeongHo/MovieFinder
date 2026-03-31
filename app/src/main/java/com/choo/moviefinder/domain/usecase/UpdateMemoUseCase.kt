package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MemoRepository
import javax.inject.Inject

class UpdateMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    // 메모 내용을 수정한다
    suspend operator fun invoke(memoId: Long, content: String) =
        repository.updateMemo(memoId, content)
}
