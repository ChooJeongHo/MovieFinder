package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.Memo
import kotlinx.coroutines.flow.Flow

interface MemoRepository {

    // 영화의 메모 목록을 Flow로 반환한다
    fun getMemos(movieId: Int): Flow<List<Memo>>

    // 영화에 메모를 저장한다
    suspend fun saveMemo(movieId: Int, content: String)

    // 메모 내용을 수정한다
    suspend fun updateMemo(memoId: Long, content: String)

    // 메모를 삭제한다
    suspend fun deleteMemo(memoId: Long)
}
