package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    // 영화의 메모 목록을 최신순으로 실시간 관찰
    @Query("SELECT * FROM memos WHERE movieId = :movieId ORDER BY updatedAt DESC")
    fun getMemosByMovieId(movieId: Int): Flow<List<MemoEntity>>

    // 메모 삽입
    @Insert
    suspend fun insert(memo: MemoEntity)

    // 메모 내용 수정 (updatedAt도 갱신)
    @Query("UPDATE memos SET content = :content, updatedAt = :updatedAt WHERE id = :memoId")
    suspend fun updateMemo(memoId: Long, content: String, updatedAt: Long = System.currentTimeMillis())

    // 메모 삭제
    @Query("DELETE FROM memos WHERE id = :memoId")
    suspend fun deleteMemo(memoId: Long)
}
