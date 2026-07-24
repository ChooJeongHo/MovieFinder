package com.choo.moviefinder.rag.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CodeChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<CodeChunk>)

    @Query("DELETE FROM code_chunks")
    suspend fun clearAll()

    /** 특정 타입의 청크만 삭제 - 전체 재인덱싱 없이 해당 타입만 부분 갱신할 때 사용(예: 성능 청크만 갱신). */
    @Query("DELETE FROM code_chunks WHERE chunkType = :type")
    suspend fun deleteByType(type: ChunkType)

    @Query("SELECT * FROM code_chunks")
    suspend fun getAll(): List<CodeChunk>

    @Query("SELECT COUNT(*) FROM code_chunks")
    suspend fun count(): Int
}
