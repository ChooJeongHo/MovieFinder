package com.choo.moviefinder.rag.performance

import com.choo.moviefinder.rag.chunk.RawChunk
import com.choo.moviefinder.rag.db.ChunkType
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * gfxinfo 프레임 성능 측정 결과(JSON, `performance-data/screen_performance.json`)를 파싱해
 * 화면(Fragment) 단위 요약을 [RawChunk](ChunkType.PERFORMANCE_METRIC)로 만든다.
 * 소스 코드 청크와 같은 테이블에 들어가므로 형식(파일 경로 + 자연어 요약)을 맞춘다.
 *
 * 이 JSON은 gradle task로 자동 생성되지 않는다(디바이스/에뮬레이터에서 실제 스크롤 조작이 필요하므로
 * JaCoCo 리포트처럼 완전 자동화 불가) - `mcp__moviefinder__get_performance`(gfxinfo reset -> 스크롤 ->
 * 파싱) 또는 `adb shell dumpsys gfxinfo <pkg> reset` 후 스크롤 -> `adb shell dumpsys gfxinfo <pkg>`로
 * 측정해 수동으로 갱신한다.
 */
object PerformanceMetricParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonFile: File): List<RawChunk> {
        val root = json.parseToJsonElement(jsonFile.readText()).jsonObject
        val capturedAt = root["capturedAt"]?.jsonPrimitive?.contentOrNull ?: "알 수 없음"
        val device = root["device"]?.jsonPrimitive?.contentOrNull ?: "알 수 없음"
        val scrollCount = root["scrollCount"]?.jsonPrimitive?.int ?: 0
        val targetFrameTimeMs = root["targetFrameTimeMs"]?.jsonPrimitive?.int ?: 16

        return root["screens"]?.jsonArray.orEmpty().map { element ->
            toChunk(element.jsonObject, capturedAt, device, scrollCount, targetFrameTimeMs)
        }
    }

    private fun toChunk(
        screen: JsonObject,
        capturedAt: String,
        device: String,
        scrollCount: Int,
        targetFrameTimeMs: Int,
    ): RawChunk {
        val screenName = screen["screen"]!!.jsonPrimitive.content
        val sourceFile = screen["sourceFile"]!!.jsonPrimitive.content
        val totalFrames = screen["totalFrames"]!!.jsonPrimitive.int
        val jankyFrames = screen["jankyFrames"]!!.jsonPrimitive.int
        val jankyPercent = screen["jankyPercent"]!!.jsonPrimitive.double
        val p50 = screen["p50Ms"]!!.jsonPrimitive.int
        val p90 = screen["p90Ms"]!!.jsonPrimitive.int
        val p95 = screen["p95Ms"]!!.jsonPrimitive.int
        val p99 = screen["p99Ms"]!!.jsonPrimitive.int
        val slowUiThread = screen["slowUiThread"]!!.jsonPrimitive.int
        val missedVsync = screen["missedVsync"]!!.jsonPrimitive.int
        val frameDeadlineMissed = screen["frameDeadlineMissed"]!!.jsonPrimitive.int
        val ratio = p50.toDouble() / targetFrameTimeMs

        val content = buildString {
            appendLine(sourceFile)
            appendLine()
            appendLine(
                "$screenName 화면 gfxinfo 프레임 성능 측정 (스크롤 ${scrollCount}회, 총 ${totalFrames}프레임, " +
                    "측정일 $capturedAt, $device)",
            )
            appendLine("50th percentile 프레임 시간 ${p50}ms, 90th ${p90}ms, 95th ${p95}ms, 99th ${p99}ms")
            appendLine(
                "Janky frame 비율 ${"%.1f".format(jankyPercent)}% ($jankyFrames/$totalFrames), " +
                    "목표 프레임 시간(${targetFrameTimeMs}ms) 대비 50th percentile ${"%.1f".format(ratio)}배 초과",
            )
            append(
                "Slow UI thread ${slowUiThread}회, Missed Vsync ${missedVsync}회, " +
                    "Frame deadline missed ${frameDeadlineMissed}회",
            )
        }
        return RawChunk(sourceFile, content, ChunkType.PERFORMANCE_METRIC)
    }
}
