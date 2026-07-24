package com.choo.moviefinder.rag.search

import com.choo.moviefinder.rag.db.CodeChunk
import com.choo.moviefinder.rag.db.FloatArrayCodec
import kotlin.math.sqrt

data class ScoredChunk(val chunk: CodeChunk, val score: Float)

/**
 * Room에서 가져온 전체 청크를 메모리에 올려두고 Kotlin에서 직접 코사인 유사도를
 * brute-force로 계산한다(별도 벡터 인덱스/서버 없음). 이 방식의 한계는 README/작업5 참고.
 */
object CosineSimilaritySearch {
    fun topK(query: FloatArray, chunks: List<CodeChunk>, k: Int): List<ScoredChunk> {
        return chunks
            .map { chunk -> ScoredChunk(chunk, cosine(query, FloatArrayCodec.toFloatArray(chunk.embedding))) }
            .sortedByDescending { it.score }
            .take(k)
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "차원이 다른 벡터는 비교할 수 없음: ${a.size} vs ${b.size}" }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }
}
