package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val category: String,
    val nextKey: Int?,
    val lastUpdated: Long = System.currentTimeMillis()
)
