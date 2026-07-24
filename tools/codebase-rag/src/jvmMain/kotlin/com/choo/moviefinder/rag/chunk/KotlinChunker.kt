package com.choo.moviefinder.rag.chunk

import com.choo.moviefinder.rag.db.ChunkType
import java.io.File

data class RawChunk(
    val filePath: String,
    val content: String,
    val type: ChunkType,
)

/**
 * 정규식 + 중괄호 깊이 매칭 기반의 경량 청커. 실제 Kotlin 파서(PSI)를 쓰지 않으므로
 * 문자열 리터럴/주석 안의 중괄호까지 깊이 계산에 포함되는 등 완벽하지 않지만,
 * 이 프로젝트 코드 스타일(중괄호를 문자열 리터럴 안에 거의 안 씀, 템플릿 `${'$'}{...}`는
 * 실제로도 중첩 스코프이므로 오히려 정확)에서는 실용적으로 충분하다.
 */
object KotlinChunker {
    private val CLASS_DECL = Regex("""\b(class|object|interface)\s+(\w+)""")
    private val FUN_DECL = Regex("""\bfun\s+(<[^>]*>\s*)?(\w+\.)?(\w+)\s*\(""")

    fun chunkFile(file: File, relativePath: String): List<RawChunk> {
        val lines = file.readLines()
        if (lines.isEmpty()) return emptyList()
        val depths = computeDepths(lines)

        val chunks = mutableListOf<RawChunk>()
        chunks += RawChunk(relativePath, fileOverview(relativePath, lines), ChunkType.FILE_OVERVIEW)

        var currentClassName: String? = null
        var idx = 0
        while (idx < lines.size) {
            val line = lines[idx]
            val trimmed = line.trimStart()
            if (depths[idx] == 0) currentClassName = null

            val classMatch = if (depths[idx] == 0 && !trimmed.startsWith("//")) CLASS_DECL.find(line) else null
            val funMatch = if (!trimmed.startsWith("//")) FUN_DECL.find(line) else null

            when {
                classMatch != null -> {
                    currentClassName = classMatch.groupValues[2]
                    val sigEnd = findSignatureEnd(lines, idx)
                    val contentStart = findLeadingContextStart(lines, idx)
                    val content = lines.subList(contentStart, sigEnd + 1).joinToString("\n")
                    chunks += RawChunk(relativePath, "$relativePath\n\n$content", ChunkType.CLASS)
                    idx = sigEnd + 1
                }
                funMatch != null -> {
                    val baseDepth = depths[idx]
                    val end = findBlockEnd(lines, depths, idx, baseDepth)
                    val contentStart = findLeadingContextStart(lines, idx)
                    val header = buildString {
                        append(relativePath)
                        if (currentClassName != null) append(" (class $currentClassName)")
                    }
                    val content = lines.subList(contentStart, end + 1).joinToString("\n")
                    chunks += RawChunk(relativePath, "$header\n\n$content", ChunkType.FUNCTION)
                    idx = end + 1
                }
                else -> idx++
            }
        }
        return chunks
    }

    /** depths[i] = i번째 줄이 "쓰여진" 시점의 중괄호 깊이 (그 줄 자신의 중괄호 반영 전). */
    private fun computeDepths(lines: List<String>): IntArray {
        val depths = IntArray(lines.size + 1)
        var d = 0
        for (i in lines.indices) {
            depths[i] = d
            d += lines[i].count { it == '{' } - lines[i].count { it == '}' }
        }
        depths[lines.size] = d
        return depths
    }

    /** declLine 바로 위에 연속된 KDoc/라인주석/annotation 블록이 있으면 그 시작 줄까지 포함시킨다.
     * Korean 주석("// 즐겨찾기 토글" 등)이 코드 바로 위에 붙는 이 프로젝트 스타일에서, 이 블록이
     * 빠지면 한글 질문과 매칭될 토큰이 거의 사라진다. */
    private fun findLeadingContextStart(lines: List<String>, declLine: Int): Int {
        var start = declLine
        var i = declLine - 1
        while (i >= 0) {
            val t = lines[i].trim()
            val isCommentOrAnnotation = t.startsWith("//") || t.startsWith("*") ||
                t.startsWith("/*") || t.startsWith("@")
            if (!isCommentOrAnnotation) break
            start = i
            i--
        }
        return start
    }

    private fun findSignatureEnd(lines: List<String>, start: Int): Int {
        for (i in start until minOf(lines.size, start + 60)) {
            if (lines[i].contains('{')) return i
        }
        return start
    }

    private fun findBlockEnd(lines: List<String>, depths: IntArray, start: Int, baseDepth: Int): Int {
        var sawOpenBrace = false
        val limit = minOf(lines.size, start + 400)
        for (i in start until limit) {
            if (lines[i].contains('{')) sawOpenBrace = true
            if (sawOpenBrace && depths[i + 1] == baseDepth) return i
        }
        // 본문 없는 선언(추상 fun, 인터페이스 시그니처, single-expression body)의 폴백:
        // 같은 깊이가 유지되는 다음 빈 줄 전까지만 포함.
        var i = start
        while (i + 1 < limit && lines[i + 1].isNotBlank() && depths[i + 1] == baseDepth) i++
        return i
    }

    private fun fileOverview(relativePath: String, lines: List<String>): String {
        val header = lines.filter {
            val t = it.trim()
            t.startsWith("package ") || t.startsWith("import ") ||
                t.startsWith("class ") || t.startsWith("object ") || t.startsWith("interface ") ||
                CLASS_DECL.containsMatchIn(t) || FUN_DECL.containsMatchIn(t)
        }
        return "$relativePath\n\n" + header.joinToString("\n")
    }
}
