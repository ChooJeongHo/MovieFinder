package com.choo.moviefinder.rag.answer

import com.choo.moviefinder.rag.search.ScoredChunk
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** 실제 Anthropic Messages API를 호출해 검색된 코드 청크를 컨텍스트로 답변을 생성한다. */
class ClaudeAnswerProvider(
    private val apiKey: String,
    private val model: String = System.getenv("ANTHROPIC_MODEL") ?: "claude-sonnet-5",
) : AnswerProvider {

    override val name: String = "claude-api-$model"

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    override fun answer(question: String, context: List<ScoredChunk>): String {
        val prompt = buildPrompt(question, context)
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", 4096)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val requestBody = Json.encodeToString(JsonObject.serializer(), body)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("Claude API 호출 실패 (HTTP ${response.statusCode()}): ${response.body().take(500)}")
        }

        val json = Json.parseToJsonElement(response.body()).jsonObject
        val contentArray: JsonArray = json["content"]?.jsonArray
            ?: error("응답에 content 필드 없음: ${response.body().take(500)}")
        return contentArray.joinToString("\n") { it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }
    }

    private fun buildPrompt(question: String, context: List<ScoredChunk>): String {
        val contextBlock = context.withIndex().joinToString("\n\n") { (i, scored) ->
            val similarity = "%.3f".format(scored.score)
            val header = "### 청크 ${i + 1} (유사도 $similarity, ${scored.chunk.filePath})"
            "$header\n```kotlin\n${scored.chunk.chunkContent}\n```"
        }
        val instructions = """
            당신은 MovieFinder라는 Android(Kotlin, Clean Architecture) 코드베이스에 대한 질문에 답하는 어시스턴트입니다.
            아래는 질문과 관련도가 높다고 검색된 코드 청크들입니다. 이 컨텍스트만 근거로 답변하고,
            컨텍스트에 없는 내용은 추측하지 말고 모른다고 답하세요. 관련 파일 경로를 답변에 함께 인용하세요.
        """.trimIndent()
        val rankingGuard = if (isRankingOrFrequencyQuestion(question)) "\n\n$RANKING_GUARD_NOTE" else ""
        return "$instructions$rankingGuard\n\n[검색된 코드 청크]\n$contextBlock\n\n[질문]\n$question"
    }

    private fun isRankingOrFrequencyQuestion(question: String): Boolean =
        RANKING_OR_FREQUENCY_KEYWORDS.any { question.contains(it) }

    companion object {
        // 079/085/107일차에 반복된 "유사도≠랭킹" 함정: 최상급/빈도 표현이 오면 모델이
        // "유사도 1위 청크"를 실제 발생 빈도·순위 1위로 착각해 단정적으로 답하는 경향이 있었음.
        private val RANKING_OR_FREQUENCY_KEYWORDS = listOf(
            "가장 많이", "가장 자주", "제일 많이", "제일 자주", "최다", "가장 흔",
            "가장 잦", "몇 번", "빈도", "누적 횟수", "누적 건수", "제일 많은", "가장 많은",
        )

        private val RANKING_GUARD_NOTE = """

            [중요: 랭킹/빈도 질문 감지됨]
            이 질문은 "가장 많이/자주" 같은 최상급 또는 빈도 표현을 포함합니다. 이 인덱스는
            현재 시점 코드의 스냅샷(청크)만 담고 있으며, 커밋 이력이나 버그 발생 횟수 등
            "얼마나 자주/많이 발생했는가"를 집계한 데이터는 포함하지 않습니다.
            검색된 청크의 유사도 순위를 발생 빈도나 실제 순위로 절대 착각하지 마세요.
            빈도·횟수·최다를 실제로 집계한 근거가 컨텍스트에 없다면, 추측해서 단정하지 말고
            "이 인덱스는 코드 스냅샷만 포함하며 발생 빈도 집계는 불가능하다"고 명시하세요.

        """.trimIndent()
    }
}
