package com.choo.moviefinder.rag.db

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 임베딩 벡터(FloatArray) <-> Room BLOB 컬럼(ByteArray) 변환.
 * CodeChunk.embedding 자체는 Room이 네이티브로 지원하는 ByteArray로 저장하지만,
 * 임베딩/검색 코드는 FloatArray로 다루므로 그 경계에서 이 변환기를 사용한다.
 */
object FloatArrayCodec {
    @TypeConverter
    @JvmStatic
    fun fromFloatArray(value: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(value.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        value.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    @TypeConverter
    @JvmStatic
    fun toFloatArray(value: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(value.size / Float.SIZE_BYTES) { buffer.float }
    }
}

object ChunkTypeConverter {
    @TypeConverter
    @JvmStatic
    fun fromChunkType(value: ChunkType): String = value.name

    @TypeConverter
    @JvmStatic
    fun toChunkType(value: String): ChunkType = ChunkType.valueOf(value)
}
