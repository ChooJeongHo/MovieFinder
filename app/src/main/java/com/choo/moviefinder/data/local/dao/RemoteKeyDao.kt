package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.RemoteKeyEntity

@Dao
interface RemoteKeyDao {

    @Query("SELECT * FROM remote_keys WHERE category = :category")
    suspend fun getRemoteKey(category: String): RemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: RemoteKeyEntity)

    @Query("DELETE FROM remote_keys WHERE category = :category")
    suspend fun clearByCategory(category: String)
}
