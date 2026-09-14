package com.choo.moviefinder.rag

import com.choo.moviefinder.rag.chunk.KotlinChunker
import com.choo.moviefinder.rag.chunk.RawChunk
import com.choo.moviefinder.rag.coverage.JacocoCoverageParser
import com.choo.moviefinder.rag.db.CodeChunk
import com.choo.moviefinder.rag.db.CodeChunkDao
import com.choo.moviefinder.rag.db.FloatArrayCodec
import com.choo.moviefinder.rag.db.openCodeIndexDatabase
import com.choo.moviefinder.rag.embedding.EmbeddingProvider
import com.choo.moviefinder.rag.embedding.EmbeddingProviderFactory
import com.choo.moviefinder.rag.embedding.VoyageEmbeddingProvider
import com.choo.moviefinder.rag.performance.PerformanceMetricParser
import java.io.File
import kotlinx.coroutines.runBlocking

private val DEFAULT_INDEXED_PATHS = listOf("domain", "presentation", "data", "core")

/**
 * sourceRoot 바로 아래(하위 디렉터리 없이)에 있는 최상위 .kt 파일. indexedPaths 스캔 대상이 아니라서
 * 별도로 챙겨야 함 — MainActivity.kt의 딥링크 백스택 처리(103/104/108/109일차)가 대표적인 예.
 */
private val TOP_LEVEL_FILES = listOf("MainActivity.kt", "MovieFinderApp.kt")
private const val SOURCE_PREFIX = "app/src/main/java/com/choo/moviefinder"
private const val JACOCO_XML_PATH = "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
private const val PERFORMANCE_JSON_PATH = "tools/codebase-rag/performance-data/screen_performance.json"

/**
 * 임베딩 직후 배치 단위로 즉시 Room에 저장하는 크기. VoyageEmbeddingProvider의 내부 배치(10청크/API 호출)와
 * 맞춰서, 배치 1개 = API 호출 1회 = DB 저장 1회가 되게 함 — 이러면 장시간 실행 중 나중에(네트워크 순단,
 * 의존성 캐시 깨짐 등) 실패해도 그 시점까지 처리한 청크는 DB에 남고, 배치마다 진행 로그도 찍힌다.
 */
private const val DB_WRITE_BATCH_SIZE = 10

private data class Timings(val walkMs: Long, val chunkMs: Long, val embedAndSaveMs: Long) {
    val totalMs get() = walkMs + chunkMs + embedAndSaveMs
}

/** args가 있으면 그 하위 경로들만(예: "domain/repository", "data/repository") 샘플 인덱싱한다. 없으면 전체 스캔. */
fun main(args: Array<String>) = runBlocking {
    val isFullScan = args.none { it.isNotBlank() }
    val indexedPaths = args.filter { it.isNotBlank() }.ifEmpty { DEFAULT_INDEXED_PATHS }

    val projectRoot = File(".").canonicalFile
    val sourceRoot = File(projectRoot, SOURCE_PREFIX)
    require(sourceRoot.exists()) { "소스 루트를 못 찾음: ${sourceRoot.absolutePath} (Gradle task로 실행했는지 확인)" }

    val walkStart = System.nanoTime()
    val ktFiles = scanKtFiles(sourceRoot, indexedPaths) +
        if (isFullScan) scanTopLevelFiles(sourceRoot) else emptyList()
    val walkMs = elapsedMs(walkStart)

    val chunkStart = System.nanoTime()
    val sourceChunks = buildRawChunks(ktFiles, projectRoot)
    val coverageChunks = buildCoverageChunks(projectRoot, indexedPaths)
    val performanceChunks = buildPerformanceChunks(projectRoot, indexedPaths)
    val rawChunks = sourceChunks + coverageChunks + performanceChunks
    val chunkMs = elapsedMs(chunkStart)

    val embedder = EmbeddingProviderFactory.create()
    val db = openCodeIndexDatabase(projectRoot)
    val dao = db.codeChunkDao()

    // 직전 시도가 중간에 죽었다면(외부 프로세스 kill 등) 이미 저장된 만큼은 건너뛰고 이어서 진행한다.
    // rawChunks 순서(파일 스캔 정렬 + 청커 순서)가 동일 스코프에서는 결정적이므로 앞에서부터 스킵하면 안전함.
    val alreadySaved = dao.count()
    val resuming = alreadySaved in 1 until rawChunks.size
    if (!resuming) dao.clearAll()
    val remainingChunks = if (resuming) rawChunks.drop(alreadySaved) else rawChunks
    if (resuming) {
        System.err.println("이전 시도에서 저장된 ${alreadySaved}개 청크 발견 - 이어서 진행 (남음: ${remainingChunks.size})")
    }

    val embedAndSaveStart = System.nanoTime()
    val resumedCount = if (resuming) alreadySaved else 0
    val savedCount = resumedCount + embedAndSaveIncrementally(embedder, dao, remainingChunks)
    val embedAndSaveMs = elapsedMs(embedAndSaveStart)

    printSummary(
        embedder,
        indexedPaths,
        ktFiles.size,
        sourceChunks.size,
        coverageChunks.size,
        performanceChunks.size,
        savedCount,
        Timings(walkMs, chunkMs, embedAndSaveMs),
    )
}

/** rawChunks를 DB_WRITE_BATCH_SIZE 단위로 임베딩 + 즉시 저장하며 진행 로그를 찍는다. 이번 호출에서 저장한 청크 수를 반환. */
private suspend fun embedAndSaveIncrementally(
    embedder: EmbeddingProvider,
    dao: CodeChunkDao,
    rawChunks: List<RawChunk>,
): Int {
    val batches = rawChunks.chunked(DB_WRITE_BATCH_SIZE)
    var savedCount = 0
    batches.forEachIndexed { index, batch ->
        val codeChunks = embedChunks(embedder, batch)
        dao.insertAll(codeChunks)
        savedCount += codeChunks.size
        System.err.println("임베딩+저장 진행: $savedCount/${rawChunks.size}청크 (배치 ${index + 1}/${batches.size})")
    }
    return savedCount
}

private fun scanKtFiles(sourceRoot: File, indexedPaths: List<String>): List<File> = indexedPaths
    .map { File(sourceRoot, it) }
    .filter { it.exists() }
    .flatMap { it.walkTopDown().filter { f -> f.isFile && f.extension == "kt" }.toList() }
    .sortedBy { it.path }

private fun scanTopLevelFiles(sourceRoot: File): List<File> = TOP_LEVEL_FILES
    .map { File(sourceRoot, it) }
    .filter { it.exists() }

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
        |임베딩+Room 저장(배치별 즉시 반영): ${t.embedAndSaveMs}ms
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
