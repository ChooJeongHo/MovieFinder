package com.choo.moviefinder.presentation.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

// 세로/가로 fragment_search.xml은 같은 FragmentSearchBinding으로 합쳐진다. 한쪽에만 있는 id는 필드를 @Nullable로,
// 뷰 클래스가 다른 id는 공통 부모 타입(View)으로 뭉개므로 SearchFragment가 컴파일은 되지만 조용히 약해진다 — 그걸 고정한다.
class SearchLayoutConsistencyTest {

    private val portrait = parse("src/main/res/layout/fragment_search.xml")
    private val landscape = parse("src/main/res/layout-land/fragment_search.xml")

    @Test
    fun `portrait and landscape search layouts declare the same ids with the same view classes`() {
        val portraitIds = portrait.idEntries()
        val landscapeIds = landscape.idEntries()

        assertTrue("세로 레이아웃에서 id를 하나도 못 읽었다(파서/경로 확인)", portraitIds.isNotEmpty())
        assertEquals(
            "세로에만 있음: ${portraitIds.missingFrom(landscapeIds)} / 가로에만 있음: ${landscapeIds.missingFrom(portraitIds)}",
            portraitIds,
            landscapeIds
        )
    }

    @Test
    fun `includes in both orientations point to the same layouts`() {
        val portraitIncludes = portrait.includeTargets()
        val landscapeIncludes = landscape.includeTargets()

        assertTrue("세로 레이아웃에서 include를 하나도 못 읽었다(파서/경로 확인)", portraitIncludes.isNotEmpty())
        assertEquals(
            "세로에만 있음: ${portraitIncludes.missingFrom(landscapeIncludes)} / " +
                "가로에만 있음: ${landscapeIncludes.missingFrom(portraitIncludes)}",
            portraitIncludes,
            landscapeIncludes
        )
    }

    // baseline-prof.txt가 FragmentSearchBinding.getRoot()를 LinearLayout으로 고정해 둬서 루트 클래스가 바뀌면 프로파일이 어긋난다
    @Test
    fun `root view class is identical so the binding root type stays LinearLayout`() {
        assertEquals("LinearLayout", portrait.documentElement.tagName)
        assertEquals(portrait.documentElement.tagName, landscape.documentElement.tagName)
    }

    // 가로 루트의 baselineAligned 기본값(true)이면 LinearLayout이 width=0dp+weight 자식(결과 패널)을 UNSPECIFIED 높이로 선측정해
    // 안의 ComposeView/LazyVerticalGrid가 무한 높이를 받고 "Vertically scrollable component was measured with an infinity
    // maximum height constraints"로 죽는다(기기에서 재현). 선언이 사라지는 회귀를 고정한다.
    @Test
    fun `landscape root disables baseline alignment so the weighted panel is not measured with unspecified height`() {
        assertEquals("false", landscape.documentElement.getAttributeNS(ANDROID_NS, "baselineAligned"))
    }

    private fun parse(path: String): Document {
        val file = File(path)
        // assumeTrue로 건너뛰면 파일이 없을 때 조용히 통과하므로 실패시킨다(Gradle 테스트 workingDir = app/)
        assertTrue("$path 파일이 없다 (workingDir=${File("").absolutePath})", file.exists())
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            isXIncludeAware = false
            isExpandEntityReferences = false
            isNamespaceAware = true
        }
        return factory.newDocumentBuilder().parse(file)
    }

    // (id, 태그명[, include 대상]) 목록 — 맵이 아니라 정렬된 목록이라 중복 id도 차이로 잡힌다
    private fun Document.idEntries(): List<String> = allElements().mapNotNull { element ->
        val id = element.getAttributeNS(ANDROID_NS, "id").removePrefix("@+id/").removePrefix("@id/")
        if (id.isEmpty()) return@mapNotNull null
        val target = if (element.tagName == "include") " ${element.getAttribute("layout")}" else ""
        "$id <${element.tagName}$target>"
    }.sorted()

    private fun Document.includeTargets(): List<String> =
        allElements().filter { it.tagName == "include" }.map { it.getAttribute("layout") }.sorted()

    private fun Document.allElements(): List<Element> {
        val nodes = getElementsByTagName("*")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    // 중복까지 세는 차집합(List.minus는 같은 값을 전부 지운다)
    private fun List<String>.missingFrom(other: List<String>): List<String> {
        val rest = other.toMutableList()
        return filterNot { rest.remove(it) }
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
