package com.choo.moviefinder.rag

import com.choo.moviefinder.rag.answer.AnswerProvider
import com.choo.moviefinder.rag.answer.ClaudeAnswerProvider
import com.choo.moviefinder.rag.answer.OfflineAnswerProvider
import com.choo.moviefinder.rag.db.ChunkType
import com.choo.moviefinder.rag.db.openCodeIndexDatabase
import com.choo.moviefinder.rag.embedding.EmbeddingProviderFactory
import com.choo.moviefinder.rag.search.CosineSimilaritySearch
import java.io.File
import kotlinx.coroutines.runBlocking

private const val TOP_K = 5

fun main(args: Array<String>) = runBlocking {
    val question = args.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: error("질문이 비어있음. 예: ./gradlew :tools:codebase-rag:ragAsk -Pquestion=\"...\"")
    val typeFilter = parseTypeFilter(args.getOrNull(1))

    val projectRoot = File(".").canonicalFile
    val db = openCodeIndexDatabase(projectRoot)
    val dao = db.codeChunkDao()

    val embedder = EmbeddingProviderFactory.create()
    val answerProvider: AnswerProvider = System.getenv("ANTHROPIC_API_KEY")
        ?.takeIf { it.isNotBlank() }
        ?.let { ClaudeAnswerProvider(apiKey = it) }
        ?: OfflineAnswerProvider()

    val totalStart = System.nanoTime()

    val embedStart = System.nanoTime()
    val queryVector = embedder.embedQuery(question)
    val embedMs = elapsedMs(embedStart)

    val loadStart = System.nanoTime()
    val allChunks = dao.getAll()
        .let { chunks -> typeFilter?.let { types -> chunks.filter { it.chunkType in types } } ?: chunks }
    val loadMs = elapsedMs(loadStart)
    require(allChunks.isNotEmpty()) {
        if (typeFilter != null) {
            "타입 필터(${typeFilter.joinToString()})에 해당하는 청크가 없음"
        } else {
            "인덱스가 비어있음. 먼저 ./gradlew :tools:codebase-rag:ragIndex 실행 필요"
        }
    }

    val searchStart = System.nanoTime()
    val topChunks = CosineSimilaritySearch.topK(queryVector, allChunks, TOP_K)
    val searchMs = elapsedMs(searchStart)

    val answerStart = System.nanoTime()
    val answer = answerProvider.answer(question, topChunks)
    val answerMs = elapsedMs(answerStart)

    val totalMs = elapsedMs(totalStart)

    println("=== 질문 ===\n$question\n")
    println("임베딩 제공자: ${embedder.name}")
    println("타입 필터: ${typeFilter?.joinToString() ?: "없음(전체 타입)"}\n")
    println("=== 검색된 청크 (top $TOP_K, 전체 ${allChunks.size}개 중) ===")
    topChunks.forEachIndexed { i, scored ->
        val similarity = "%.4f".format(scored.score)
        println("${i + 1}. [${scored.chunk.chunkType}] ${scored.chunk.filePath} (유사도 $similarity)")
    }
    println("\n=== 답변 (${answerProvider.name}) ===\n$answer\n")
    println(
        """
        |=== 소요 시간 ===
        |질문 임베딩          : ${embedMs}ms
        |Room 전체 로드       : ${loadMs}ms (${allChunks.size}개 청크)
        |코사인 유사도 검색   : ${searchMs}ms
        |답변 생성            : ${answerMs}ms
        |총 소요 시간         : ${totalMs}ms
        """.trimMargin(),
    )
}

private fun elapsedMs(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000

/** "-Ptypes=\"CLASS,FUNCTION\"" 처럼 쉼표로 구분된 ChunkType 이름을 파싱. 없거나 빈 값이면 필터 없음(null). */
private fun parseTypeFilter(raw: String?): Set<ChunkType>? {
    val names = raw?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: return null
    if (names.isEmpty()) return null
    return names.map { name ->
        runCatching { ChunkType.valueOf(name.uppercase()) }
            .getOrElse { error("알 수 없는 chunkType: \"$name\" (가능한 값: ${ChunkType.entries.joinToString()})") }
    }.toSet()
}
