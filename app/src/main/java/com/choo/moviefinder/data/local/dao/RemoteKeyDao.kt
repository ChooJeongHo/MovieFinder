package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.RemoteKeyEntity

@Dao
interface RemoteKeyDao {

    // 카테고리에 해당하는 원격 페이징 키 조회
    @Query("SELECT * FROM remote_keys WHERE category = :category")
    suspend fun getRemoteKey(category: String): RemoteKeyEntity?

    // 원격 페이징 키 삽입 (중복 시 교체)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: RemoteKeyEntity)

    // 특정 카테고리의 원격 키 삭제
    @Query("DELETE FROM remote_keys WHERE category = :category")
    suspend fun clearByCategory(category: String)
}
