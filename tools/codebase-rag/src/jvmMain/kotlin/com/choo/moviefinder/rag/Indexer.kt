package com.choo.moviefinder.rag

import com.choo.moviefinder.rag.chunk.KotlinChunker
import com.choo.moviefinder.rag.chunk.RawChunk
import com.choo.moviefinder.rag.coverage.JacocoCoverageParser
import com.choo.moviefinder.rag.db.CodeChunk
import com.choo.moviefinder.rag.db.FloatArrayCodec
import com.choo.moviefinder.rag.db.openCodeIndexDatabase
import com.choo.moviefinder.rag.embedding.EmbeddingProvider
import com.choo.moviefinder.rag.embedding.EmbeddingProviderFactory
import com.choo.moviefinder.rag.embedding.VoyageEmbeddingProvider
import com.choo.moviefinder.rag.performance.PerformanceMetricParser
import java.io.File
import kotlinx.coroutines.runBlocking

private val DEFAULT_INDEXED_PATHS = listOf("domain", "presentation", "data")
private const val SOURCE_PREFIX = "app/src/main/java/com/choo/moviefinder"
private const val JACOCO_XML_PATH = "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
private const val PERFORMANCE_JSON_PATH = "tools/codebase-rag/performance-data/screen_performance.json"

private data class Timings(val walkMs: Long, val chunkMs: Long, val embedMs: Long, val dbMs: Long) {
    val totalMs get() = walkMs + chunkMs + embedMs + dbMs
}

/** args가 있으면 그 하위 경로들만(예: "domain/repository", "data/repository") 샘플 인덱싱한다. 없으면 전체 스캔. */
fun main(args: Array<String>) = runBlocking {
    val indexedPaths = args.filter { it.isNotBlank() }.ifEmpty { DEFAULT_INDEXED_PATHS }

    val projectRoot = File(".").canonicalFile
    val sourceRoot = File(projectRoot, SOURCE_PREFIX)
    require(sourceRoot.exists()) { "소스 루트를 못 찾음: ${sourceRoot.absolutePath} (Gradle task로 실행했는지 확인)" }

    val walkStart = System.nanoTime()
    val ktFiles = scanKtFiles(sourceRoot, indexedPaths)
    val walkMs = elapsedMs(walkStart)

    val chunkStart = System.nanoTime()
    val sourceChunks = buildRawChunks(ktFiles, projectRoot)
    val coverageChunks = buildCoverageChunks(projectRoot, indexedPaths)
    val performanceChunks = buildPerformanceChunks(projectRoot, indexedPaths)
    val rawChunks = sourceChunks + coverageChunks + performanceChunks
    val chunkMs = elapsedMs(chunkStart)

    val embedder = EmbeddingProviderFactory.create()
    val embedStart = System.nanoTime()
    val codeChunks = embedChunks(embedder, rawChunks)
    val embedMs = elapsedMs(embedStart)

    val db = openCodeIndexDatabase(projectRoot)
    val dao = db.codeChunkDao()
    val dbStart = System.nanoTime()
    dao.clearAll()
    dao.insertAll(codeChunks)
    val dbMs = elapsedMs(dbStart)
    val totalCount = dao.count()

    printSummary(
        embedder,
        indexedPaths,
        ktFiles.size,
        sourceChunks.size,
        coverageChunks.size,
        performanceChunks.size,
        totalCount,
        Timings(walkMs, chunkMs, embedMs, dbMs),
    )
}

private fun scanKtFiles(sourceRoot: File, indexedPaths: List<String>): List<File> = indexedPaths
    .map { File(sourceRoot, it) }
    .filter { it.exists() }
    .flatMap { it.walkTopDown().filter { f -> f.isFile && f.extension == "kt" }.toList() }
    .sortedBy { it.path }

private fun buildRawChunks(ktFiles: List<File>, projectRoot: File): List<RawChunk> = ktFiles.flatMap { file ->
    val relativePath = file.relativeTo(projectRoot).path
    runCatching { KotlinChunker.chunkFile(file, relativePath) }.getOrElse {
        System.err.println("청킹 실패, 건너뜀: $relativePath (${it.message})")
        emptyList()
    }
}

/** indexedPaths 스코프 밖의 파일에 대한 커버리지 청크는 만들지 않는다(소스 스코프와 항상 일치시킴). */
private fun buildCoverageChunks(projectRoot: File, indexedPaths: List<String>): List<RawChunk> {
    val xmlFile = File(projectRoot, JACOCO_XML_PATH)
    if (!xmlFile.exists()) {
        System.err.println("JaCoCo 리포트 없음, 커버리지 청크 스킵: ${xmlFile.absolutePath} (먼저 ./gradlew jacocoTestReport 실행 필요)")
        return emptyList()
    }
    val allowedPrefixes = indexedPaths.map { "$SOURCE_PREFIX/$it" }
    return runCatching { JacocoCoverageParser.parse(xmlFile, projectRoot) }
        .getOrElse {
            System.err.println("JaCoCo 리포트 파싱 실패, 커버리지 청크 스킵: ${it.message}")
            emptyList()
        }
        .filter { chunk -> allowedPrefixes.any { chunk.filePath.startsWith(it) } }
}

/** indexedPaths 스코프 밖의 화면에 대한 성능 청크는 만들지 않는다(소스 스코프와 항상 일치시킴, 커버리지와 동일 패턴). */
private fun buildPerformanceChunks(projectRoot: File, indexedPaths: List<String>): List<RawChunk> {
    val jsonFile = File(projectRoot, PERFORMANCE_JSON_PATH)
    if (!jsonFile.exists()) {
        System.err.println("성능 측정 JSON 없음, 성능 청크 스킵: ${jsonFile.absolutePath}")
        return emptyList()
    }
    val allowedPrefixes = indexedPaths.map { "$SOURCE_PREFIX/$it" }
    return runCatching { PerformanceMetricParser.parse(jsonFile) }
        .getOrElse {
            System.err.println("성능 측정 JSON 파싱 실패, 성능 청크 스킵: ${it.message}")
            emptyList()
        }
        .filter { chunk -> allowedPrefixes.any { chunk.filePath.startsWith(it) } }
}

internal fun embedChunks(embedder: EmbeddingProvider, rawChunks: List<RawChunk>): List<CodeChunk> {
    val vectors = embedder.embedDocuments(rawChunks.map { it.content })
    return rawChunks.zip(vectors).map { (raw, vector) ->
        CodeChunk(
            filePath = raw.filePath,
            chunkContent = raw.content,
            chunkType = raw.type,
            embedding = FloatArrayCodec.fromFloatArray(vector),
        )
    }
}

private fun printSummary(
    embedder: EmbeddingProvider,
    indexedPaths: List<String>,
    fileCount: Int,
    sourceChunkCount: Int,
    coverageChunkCount: Int,
    performanceChunkCount: Int,
    savedCount: Int,
    t: Timings,
) {
    println(
        """
        |=== codebase-rag 인덱싱 완료 ===
        |임베딩 제공자        : ${embedder.name} (dim=${embedder.dimension})
        |스캔 대상 경로       : ${indexedPaths.joinToString()}
        |.kt 파일 수          : $fileCount
        |소스 청크 수         : $sourceChunkCount
        |커버리지 청크 수     : $coverageChunkCount
        |성능 청크 수         : $performanceChunkCount
        |Room 저장 청크 수    : $savedCount
        |--- 소요 시간 ---
        |파일 탐색            : ${t.walkMs}ms
        |청크 분할(+커버리지/성능 파싱): ${t.chunkMs}ms
        |임베딩               : ${t.embedMs}ms
        |Room 저장(clear+insert): ${t.dbMs}ms
        |총 소요 시간         : ${t.totalMs}ms
        """.trimMargin(),
    )
    if (embedder is VoyageEmbeddingProvider) {
        val stats = embedder.stats
        println(
            """
            |--- Voyage API 사용량 ---
            |API 호출 횟수        : ${stats.apiCalls}회
            |총 토큰 사용량       : ${stats.totalTokens}
            """.trimMargin(),
        )
    }
}

private fun elapsedMs(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000
