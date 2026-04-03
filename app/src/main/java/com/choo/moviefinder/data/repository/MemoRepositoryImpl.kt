package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.MemoDao
import com.choo.moviefinder.data.local.entity.MemoEntity
import com.choo.moviefinder.domain.model.Memo
import com.choo.moviefinder.domain.model.MemoConstants
import com.choo.moviefinder.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MemoRepositoryImpl @Inject constructor(
    private val memoDao: MemoDao
) : MemoRepository {

    // 영화의 메모 목록을 최신순으로 실시간 Flow로 조회
    override fun getMemos(movieId: Int): Flow<List<Memo>> {
        require(movieId > 0) { "Movie ID must be positive" }
        return memoDao.getMemosByMovieId(movieId).map { entities ->
            entities.map {
                Memo(
                    id = it.id,
                    movieId = it.movieId,
                    content = it.content,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    // 새 메모를 저장
    override suspend fun saveMemo(movieId: Int, content: String) {
        require(movieId > 0) { "Movie ID must be positive" }
        require(content.isNotBlank()) { "Memo content must not be blank" }
        require(content.length <= MemoConstants.MAX_LENGTH) {
            "Memo content must not exceed ${MemoConstants.MAX_LENGTH} characters"
        }
        memoDao.insert(MemoEntity(movieId = movieId, content = content))
    }

    // 기존 메모 내용을 수정
    override suspend fun updateMemo(memoId: Long, content: String) {
        require(content.isNotBlank()) { "Memo content must not be blank" }
        require(content.length <= MemoConstants.MAX_LENGTH) {
            "Memo content must not exceed ${MemoConstants.MAX_LENGTH} characters"
        }
        memoDao.updateMemo(memoId = memoId, content = content, updatedAt = System.currentTimeMillis())
    }

    // 메모를 삭제
    override suspend fun deleteMemo(memoId: Long) {
        memoDao.deleteMemo(memoId)
    }
}
