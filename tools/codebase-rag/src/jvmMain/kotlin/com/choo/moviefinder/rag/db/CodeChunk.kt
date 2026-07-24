package com.choo.moviefinder.rag.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChunkType {
    CLASS,
    FUNCTION,
    FILE_OVERVIEW,

    /** JaCoCo 커버리지 리포트에서 파생된 파일당 라인 커버리지 요약 - 소스 코드가 아닌 메트릭 데이터. */
    COVERAGE_METRIC,

    /** gfxinfo 프레임 성능 측정에서 파생된 화면(Fragment)당 프레임 타이밍 요약 - 소스 코드가 아닌 메트릭 데이터. */
    PERFORMANCE_METRIC,
}

@Entity(tableName = "code_chunks")
data class CodeChunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val chunkContent: String,
    val chunkType: ChunkType,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeChunk) return false
        return id == other.id &&
            filePath == other.filePath &&
            chunkContent == other.chunkContent &&
            chunkType == other.chunkType &&
            embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + chunkContent.hashCode()
        result = 31 * result + chunkType.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
