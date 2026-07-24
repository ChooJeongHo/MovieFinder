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
            put("max_tokens", 1024)
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
        return """
            당신은 MovieFinder라는 Android(Kotlin, Clean Architecture) 코드베이스에 대한 질문에 답하는 어시스턴트입니다.
            아래는 질문과 관련도가 높다고 검색된 코드 청크들입니다. 이 컨텍스트만 근거로 답변하고,
            컨텍스트에 없는 내용은 추측하지 말고 모른다고 답하세요. 관련 파일 경로를 답변에 함께 인용하세요.

            [검색된 코드 청크]
            $contextBlock

            [질문]
            $question
        """.trimIndent()
    }
}
