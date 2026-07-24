package com.choo.moviefinder.rag.embedding

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Voyage AI(Anthropic 공식 추천 임베딩 파트너)의 voyage-code-3 모델을 실제로 호출하는 구현체.
 * input_type을 document/query로 구분해 비대칭 임베딩을 쓰고, 인덱싱 시에는 배치로 묶어 호출 수를 줄인다.
 *
 * 결제수단 미등록 계정은 3 RPM(분당 3회)으로 제한되므로, 매 호출 전에 최소 간격을 강제하고
 * 그래도 429가 나면 Retry-After(또는 기본 백오프)만큼 기다렸다가 재시도한다.
 */
class VoyageEmbeddingProvider(
    private val apiKey: String,
    private val model: String = "voyage-code-3",
    override val dimension: Int = 1024,
    // 결제수단 미등록 계정은 10K TPM 한도가 있음. 실측(90줄 코드 청크 ≈ 813 토큰) 기준 여유를 두고
    // 배치당 10청크로 제한(최악의 경우에도 대략 8~9천 토큰 선).
    private val batchSize: Int = 10,
) : EmbeddingProvider {

    override val name: String = "voyage-$model-$dimension"

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    private val apiCallCount = AtomicInteger(0)
    private val totalTokens = AtomicLong(0)
    private var lastCallAtMs = 0L

    val stats: Stats get() = Stats(apiCallCount.get(), totalTokens.get())
    data class Stats(val apiCalls: Int, val totalTokens: Long)

    override fun embedDocuments(texts: List<String>): List<FloatArray> {
        if (texts.isEmpty()) return emptyList()
        return texts.chunked(batchSize).flatMap { batch -> callApi(batch, inputType = "document") }
    }

    override fun embedQuery(text: String): FloatArray = callApi(listOf(text), inputType = "query").first()

    private fun callApi(texts: List<String>, inputType: String): List<FloatArray> {
        var attempt = 0
        while (true) {
            enforceMinInterval()
            val response = sendRequest(texts, inputType)
            lastCallAtMs = System.currentTimeMillis()

            if (response.statusCode() == 429 && attempt < MAX_RETRIES) {
                val waitMs = response.headers().firstValue("Retry-After")
                    .map { it.toLongOrNull()?.times(1000) }
                    .orElse(null) ?: DEFAULT_BACKOFF_MS
                System.err.println(
                    "Voyage API 429(rate limit), ${waitMs}ms 대기 후 재시도 (${attempt + 1}/$MAX_RETRIES)",
                )
                Thread.sleep(waitMs)
                attempt++
                continue
            }
            if (response.statusCode() !in 200..299) {
                error("Voyage API 호출 실패 (HTTP ${response.statusCode()}): ${response.body().take(500)}")
            }
            apiCallCount.incrementAndGet()
            return parseEmbeddings(response.body())
        }
    }

    private fun enforceMinInterval() {
        val waitMs = MIN_CALL_INTERVAL_MS - (System.currentTimeMillis() - lastCallAtMs)
        if (lastCallAtMs > 0 && waitMs > 0) Thread.sleep(waitMs)
    }

    private fun sendRequest(texts: List<String>, inputType: String): HttpResponse<String> {
        val requestJson = buildJsonObject {
            put("input", buildJsonArray { texts.forEach { add(it) } })
            put("model", model)
            put("input_type", inputType)
            put("output_dimension", dimension)
        }
        val requestBody = Json.encodeToString(JsonObject.serializer(), requestJson)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.voyageai.com/v1/embeddings"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun parseEmbeddings(responseBody: String): List<FloatArray> {
        val json = Json.parseToJsonElement(responseBody).jsonObject
        val usedTokens = json["usage"]?.jsonObject?.get("total_tokens")?.jsonPrimitive?.int ?: 0
        totalTokens.addAndGet(usedTokens.toLong())

        return json["data"]!!.jsonArray
            .sortedBy { it.jsonObject["index"]!!.jsonPrimitive.int }
            .map { item ->
                val embeddingArray = item.jsonObject["embedding"]!!.jsonArray
                FloatArray(embeddingArray.size) { i -> embeddingArray[i].jsonPrimitive.float }
            }
    }

    companion object {
        // 결제수단 미등록 계정 기본 한도가 3 RPM이라 20초/회 미만으로는 안전하지 않음 - 여유를 둬 21초로 고정.
        private const val MIN_CALL_INTERVAL_MS = 21_000L
        private const val DEFAULT_BACKOFF_MS = 21_000L
        private const val MAX_RETRIES = 5
    }
}
