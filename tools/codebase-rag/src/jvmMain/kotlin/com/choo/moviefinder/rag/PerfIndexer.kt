package com.choo.moviefinder.rag

import com.choo.moviefinder.rag.db.ChunkType
import com.choo.moviefinder.rag.db.openCodeIndexDatabase
import com.choo.moviefinder.rag.embedding.EmbeddingProviderFactory
import com.choo.moviefinder.rag.performance.PerformanceMetricParser
import java.io.File
import kotlinx.coroutines.runBlocking

private const val PERFORMANCE_JSON_PATH = "tools/codebase-rag/performance-data/screen_performance.json"

/**
 * PERFORMANCE_METRIC 청크만 갱신한다(전체 재인덱싱 없이 기존 소스/커버리지 청크는 그대로 둠).
 * screen_performance.json을 gfxinfo 측정으로 갱신한 뒤 이 task만 다시 돌리면 됨.
 */
fun main() = runBlocking {
    val projectRoot = File(".").canonicalFile
    val jsonFile = File(projectRoot, PERFORMANCE_JSON_PATH)
    require(jsonFile.exists()) { "성능 측정 JSON을 못 찾음: ${jsonFile.absolutePath}" }

    val rawChunks = PerformanceMetricParser.parse(jsonFile)
    require(rawChunks.isNotEmpty()) { "성능 측정 JSON에 screens 항목이 없음: ${jsonFile.absolutePath}" }

    val embedder = EmbeddingProviderFactory.create()
    val codeChunks = embedChunks(embedder, rawChunks)

    val db = openCodeIndexDatabase(projectRoot)
    val dao = db.codeChunkDao()
    dao.deleteByType(ChunkType.PERFORMANCE_METRIC)
    dao.insertAll(codeChunks)

    println(
        """
        |=== codebase-rag 성능 청크 부분 인덱싱 완료 ===
        |임베딩 제공자        : ${embedder.name} (dim=${embedder.dimension})
        |성능 청크 수         : ${codeChunks.size}
        |Room 전체 청크 수    : ${dao.count()}
        """.trimMargin(),
    )
}
