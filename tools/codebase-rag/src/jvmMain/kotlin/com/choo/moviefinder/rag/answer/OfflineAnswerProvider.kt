package com.choo.moviefinder.rag.answer

import com.choo.moviefinder.rag.search.ScoredChunk

/**
 * ANTHROPIC_API_KEY가 없을 때 쓰는 폴백. LLM 합성 없이 검색된 청크 목록만 정리해서 보여준다 —
 * 파이프라인의 검색 단계까지는 실제로 동작함을 확인할 수 있게 한다.
 */
class OfflineAnswerProvider : AnswerProvider {
    override val name: String = "offline-no-llm"

    override fun answer(question: String, context: List<ScoredChunk>): String {
        if (context.isEmpty()) return "관련 코드 청크를 찾지 못했습니다."
        return buildString {
            appendLine("[ANTHROPIC_API_KEY 미설정 - LLM 답변 합성 없이 검색된 청크만 표시]")
            appendLine("질문: $question")
            context.forEachIndexed { i, scored ->
                val similarity = "%.3f".format(scored.score)
                appendLine("${i + 1}. ${scored.chunk.filePath} (${scored.chunk.chunkType}, 유사도 $similarity)")
            }
        }.trimEnd()
    }
}
