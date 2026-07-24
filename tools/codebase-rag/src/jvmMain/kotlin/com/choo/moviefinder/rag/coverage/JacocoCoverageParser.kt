package com.choo.moviefinder.rag.coverage

import com.choo.moviefinder.rag.chunk.RawChunk
import com.choo.moviefinder.rag.db.ChunkType
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * JaCoCo XML 리포트(app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml)를 파싱해
 * 파일(sourcefile) 단위 라인 커버리지 요약을 [RawChunk](ChunkType.COVERAGE_METRIC)로 만든다.
 * 소스 코드 청크와 같은 테이블에 들어가므로 형식(파일 경로 + 자연어 요약)을 맞춘다.
 */
object JacocoCoverageParser {

    fun parse(xmlFile: File, projectRoot: File): List<RawChunk> {
        val document = newSecureDocumentBuilder().parse(xmlFile)
        val chunks = mutableListOf<RawChunk>()

        val packages = document.getElementsByTagName("package")
        for (i in 0 until packages.length) {
            val packageElement = packages.item(i) as Element
            val packagePath = packageElement.getAttribute("name") // e.g. com/choo/moviefinder/data/repository
            for (sourcefile in packageElement.children("sourcefile")) {
                toChunk(sourcefile, packagePath, projectRoot)?.let { chunks += it }
            }
        }
        return chunks
    }

    private fun toChunk(sourcefile: Element, packagePath: String, projectRoot: File): RawChunk? {
        val fileName = sourcefile.getAttribute("name")
        val lineCounter = sourcefile.children("counter")
            .firstOrNull { it.getAttribute("type") == "LINE" } ?: return null

        val missed = lineCounter.getAttribute("missed").toIntOrNull() ?: return null
        val covered = lineCounter.getAttribute("covered").toIntOrNull() ?: return null
        val total = missed + covered
        if (total == 0) return null // 라인이 없는(빈) 파일 - 정보 가치 없음

        val percent = (covered * 100.0 / total)
        val relativePath = "app/src/main/java/$packagePath/$fileName"
        val hasTestFile = File(
            projectRoot,
            relativePath.replace("src/main/java", "src/test/java").replace(".kt", "Test.kt"),
        ).exists()

        val content = buildString {
            appendLine(relativePath)
            appendLine()
            appendLine("라인 커버리지 ${"%.1f".format(percent)}% (커버 ${covered}줄 / 미커버 ${missed}줄, 전체 ${total}줄)")
            append("대응 단위 테스트 파일: ${if (hasTestFile) "있음" else "없음"}")
        }
        return RawChunk(relativePath, content, ChunkType.COVERAGE_METRIC)
    }

    private fun Element.children(tagName: String): List<Element> {
        val result = mutableListOf<Element>()
        val nodes = childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE && node.nodeName == tagName) {
                result += node as Element
            }
        }
        return result
    }

    /** JaCoCo XML의 <!DOCTYPE ... "report.dtd"> 외부 DTD를 절대 로드하지 않도록 막는다(XXE 방지 겸 오프라인 안전). */
    private fun newSecureDocumentBuilder() = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        isExpandEntityReferences = false
    }.newDocumentBuilder()
}
