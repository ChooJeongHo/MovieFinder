package com.choo.moviefinder.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

// fragment_home.xml 루트는 스크롤 없는 세로 LinearLayout이라, wrap_content 섹션(박스오피스·시청기록)이 높이를 다 가져가면
// 마지막 match_parent 목록이 0px이 됐다(실기기 SM-S926N 실측). 섹션을 접히는 AppBarLayout 자식으로 옮겨 목록에 닿을 수 있게 한
// 구조(CoordinatorLayout + AppBarLayout + appbar_scrolling_view_behavior)를 JVM에서 고정한다.
// 이 테스트는 구조 규칙만 본다 — 실제 측정·스크롤 동작은 실기기 확인이 필요하다.
//
// 규칙 일부(안쪽 앱바 위험 요소, scroll 플래그 순서, id 집합)는 `Document.xxx()` 확장 함수로 뽑아 두고, 실제 XML에는 "위반 없음"을,
// 일부러 망가뜨린 XML 사본(문자열 변형, 원본 파일은 건드리지 않음)에는 "위반 있음"을 각각 단정한다. 그래서 규칙이 조용히 항상
// 통과하는 단정으로 변해도(아래 `규칙 감도` 테스트들이) 빨갛게 바뀐다.
class HomeLayoutStructureTest {

    private val homeXml = readXml("src/main/res/layout/fragment_home.xml")
    private val home = parseXml(homeXml)

    // baseline-prof.txt가 FragmentHomeBinding.getRoot()를 LinearLayout으로 고정해 둬서 루트 클래스가 바뀌면 프로파일이 어긋난다
    @Test
    fun `root view class stays LinearLayout so the binding root type is unchanged`() {
        assertEquals("LinearLayout", home.documentElement.tagName)
    }

    // 한 번 노출한 id는 뷰 클래스까지 그대로여야 HomeFragment의 바인딩 필드 이름/타입/nullability이 유지된다
    @Test
    fun `ids that existed before the restructure keep their view classes`() {
        val current = home.idEntries()

        assertTrue("fragment_home.xml에서 id를 하나도 못 읽었다(파서/경로 확인)", current.isNotEmpty())
        assertEquals(
            "사라지거나 클래스가 바뀐 id: ${LEGACY_ID_ENTRIES.missingFrom(current)}",
            emptyList<String>(),
            LEGACY_ID_ENTRIES.missingFrom(current)
        )
    }

    // 이번 구조 변경에서 새로 생긴 id는 정확히 2개여야 한다. 기존 19개 스냅샷 + 신규 2개 = 21개와 전체 집합이 일치해야 하므로,
    // 모르는 id가 몰래 늘어도(바인딩 필드가 조용히 늘어남) 기존 id가 사라져도 실패한다.
    @Test
    fun `exactly the legacy ids plus the two new ids exist`() {
        val changes = home.idSetChanges()

        assertTrue("id 집합이 기대와 다르다: $changes", changes.isEmpty())
    }

    @Test
    fun `no id is declared twice`() {
        val duplicated = home.elements().map { it.viewId() }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys

        assertTrue("중복 선언된 id: $duplicated", duplicated.isEmpty())
    }

    // 접힘 상태 저장/복원은 두 뷰에 id가 "있기만" 하면 되고 이름은 상관없다. 이름을 바꾸면 안 되는 실제 이유는
    // HomeFragment.kt의 binding.appBarHomeSections 와 MainActivity.kt의 R.id.app_bar_home_sections 가 이 이름을 참조하기 때문이다.
    @Test
    fun `the section coordinator and section app bar exist with the expected classes`() {
        assertEquals(COORDINATOR_CLASS, home.byId("home_sections_coordinator").tagName)
        assertEquals(APP_BAR_CLASS, home.byId("app_bar_home_sections").tagName)
    }

    @Test
    fun `box office and watch history sections are scrollable children of the section app bar`() {
        val sectionAppBar = home.byId("app_bar_home_sections")

        for (sectionId in listOf("box_office_section", "watch_history_section")) {
            val section = home.byId(sectionId)
            assertTrue("$sectionId 의 부모가 app_bar_home_sections 가 아니다", section.parentElement() === sectionAppBar)
            val flags = section.appAttr("layout_scrollFlags").split('|').map { it.trim() }
            assertTrue("$sectionId 의 app:layout_scrollFlags 에 scroll 이 없다(현재: $flags)", "scroll" in flags)
        }
    }

    // AppBarLayout 규칙: scroll 플래그가 있는 자식은 플래그가 없는(고정) 자식보다 앞에 와야 한다. 지금은 두 섹션 모두 scroll이라
    // 순서가 무의미하게 통과하지만, 나중에 고정 자식(예: 항상 보이는 칩 줄)을 앞에 끼우면 이 테스트가 막는다.
    @Test
    fun `scroll flagged children come before fixed children in the section app bar`() {
        val sectionAppBar = home.byId("app_bar_home_sections")

        assertTrue("섹션 앱바의 자식을 못 읽었다", sectionAppBar.childElements().size >= 2)
        val violations = sectionAppBar.scrollOrderViolations()
        assertTrue("scroll 자식이 고정 자식 뒤에 있다: $violations", violations.isEmpty())
    }

    @Test
    fun `section app bar sits directly inside the coordinator which is a direct child of the root`() {
        val coordinator = home.byId("home_sections_coordinator")
        val sectionAppBar = home.byId("app_bar_home_sections")

        assertTrue("코디네이터는 루트의 직계 자식이어야 한다", coordinator.parentElement() === home.documentElement)
        assertTrue("섹션 앱바는 코디네이터의 직계 자식이어야 한다", sectionAppBar.parentElement() === coordinator)
    }

    // 툴바+TabLayout 앱바는 fitsSystemWindows로 상태바 인셋을 처리한다 — 코디네이터 안으로 들어가면 인셋 처리가 바뀐다
    @Test
    fun `toolbar and tab layout stay outside the coordinator in the original app bar`() {
        val coordinator = home.byId("home_sections_coordinator")
        val outerAppBar = home.byId("toolbar").parentElement()

        for (id in listOf("toolbar", "tab_layout")) {
            val view = home.byId(id)
            assertFalse("$id 가 코디네이터 안으로 들어갔다", coordinator in view.ancestors())
            assertTrue("$id 의 부모가 같은 앱바가 아니다", view.parentElement() === outerAppBar)
        }
        assertEquals(APP_BAR_CLASS, outerAppBar?.tagName)
        assertTrue("바깥 앱바는 코디네이터의 자식이 아니어야 한다", outerAppBar?.parentElement() === home.documentElement)
        assertEquals("true", outerAppBar?.androidAttr("fitsSystemWindows"))
    }

    @Test
    fun `movie list container uses the appbar scrolling view behavior and holds every list state view`() {
        val coordinator = home.byId("home_sections_coordinator")
        val listContainer = home.byId("swipe_refresh").ancestors().firstOrNull { it.parentElement() === coordinator }

        assertNotNull("swipe_refresh 가 코디네이터의 자식 안에 있지 않다", listContainer)
        listContainer!!
        assertEquals(
            "목록 컨테이너는 appbar_scrolling_view_behavior 를 써야 섹션 앱바 아래에 배치된다",
            "@string/appbar_scrolling_view_behavior",
            listContainer.appAttr("layout_behavior")
        )
        assertEquals("match_parent", listContainer.androidAttr("layout_height"))
        for (id in listOf("swipe_refresh", "rv_movies", "shimmer_view", "error_view", "empty_view", "fab_scroll_top")) {
            assertTrue("$id 가 목록 컨테이너 밖에 있다", listContainer in home.byId(id).ancestors())
        }
    }

    // rv_movies가 스크롤 뷰 안에 들어가면 RecyclerView 재활용이 꺼지고 모든 아이템이 한 번에 붙는다
    @Test
    fun `the movie list is not nested inside any scroll view`() {
        val scrollAncestors = home.byId("rv_movies").ancestors().filter { it.tagName.endsWith("ScrollView") }

        assertTrue("rv_movies 조상에 스크롤 뷰가 있다: ${scrollAncestors.map { it.tagName }}", scrollAncestors.isEmpty())
    }

    // AppBarLayout.Behavior는 wrap_content 앱바를 높이 UNSPECIFIED로 측정한다(그래야 앱바가 부모보다 커질 수 있다). 그 안의 자손은
    // "위에서 높이를 제한해 준다"는 가정이 깨지므로 아래 다섯 가지를 두지 않는다. 일반 View의 match_parent는 크래시가 아니라
    // UNSPECIFIED를 받아 내용 크기로 측정될 뿐이라, (d)·(e)는 크래시 방지가 아니라 "부모를 채운다"는 의도와 실제 측정이 어긋나는 것을
    // 막는 규칙이다. 실제로 터질 수 있는 쪽은 (a)(b)(c)다.
    //   (a) ComposeView — 안의 LazyColumn/LazyVerticalGrid가 무한 최대 높이를 받아 "infinity maximum height constraints"로 죽는다
    //       (123일차 검색 가로 모드에서 기기 재현).
    //   (b) ScrollView/NestedScrollView — 세로 스크롤 뷰가 UNSPECIFIED에선 내용 전체 높이가 되어 스크롤이 무의미해지고 앱바 드래그와 충돌한다.
    //   (c) 세로 RecyclerView — 같은 이유로 모든 아이템이 한 번에 붙어 재활용이 꺼진다.
    //   (d) layout_height=match_parent 인 뷰.
    //   (e) layout_height 가 없는 <include> — 포함된 레이아웃 루트가 match_parent 일 수 있어 위 (d)를 우회한다.
    @Test
    fun `nothing under the section app bar relies on a bounded parent height`() {
        assertEquals(
            "앱바가 wrap_content 가 아니면 UNSPECIFIED 측정 전제가 달라진다",
            "wrap_content",
            home.byId("app_bar_home_sections").androidAttr("layout_height")
        )
        val hazards = home.unboundedHeightHazards()

        assertTrue("섹션 앱바 안에 높이 제한을 가정하는 뷰가 있다: $hazards", hazards.isEmpty())
    }

    // 바깥 앱바가 상태바 인셋을 이미 처리하므로 안쪽에 또 넣으면 이중 적용된다
    @Test
    fun `section app bar does not apply system window insets again`() {
        val fitsSystemWindows = home.byId("app_bar_home_sections").androidAttr("fitsSystemWindows")

        assertTrue(
            "안쪽 앱바에 fitsSystemWindows=$fitsSystemWindows 가 있다",
            fitsSystemWindows.isEmpty() || fitsSystemWindows == "false"
        )
    }

    // 지금처럼 섹션이 윈도우 배경 위에 평평하게 그려지도록, Material 기본 앱바의 배경(colorSurface)·그림자·lift 애니메이션을 끈다
    @Test
    fun `section app bar is flat and transparent so it looks like the old plain sections`() {
        val sectionAppBar = home.byId("app_bar_home_sections")

        assertEquals("@null", sectionAppBar.androidAttr("background"))
        assertEquals("0dp", sectionAppBar.androidAttr("elevation"))
        assertEquals("@null", sectionAppBar.androidAttr("stateListAnimator"))
        assertEquals("false", sectionAppBar.appAttr("liftOnScroll"))
    }

    // 목록 컨테이너가 앱바 높이만큼 아래로 밀리므로 화면 중앙 정렬(match_parent+center)이면 에러/빈 화면이 화면 밖으로 밀린다
    @Test
    fun `error and empty state views are wrap_content and top aligned`() {
        for (id in listOf("error_view", "empty_view")) {
            val include = home.byId(id)
            assertEquals("$id 의 높이는 wrap_content 여야 한다", "wrap_content", include.androidAttr("layout_height"))
            assertEquals("$id 의 너비는 match_parent 여야 한다", "match_parent", include.androidAttr("layout_width"))
            val gravity = include.androidAttr("layout_gravity").split('|').map { it.trim() }
            assertTrue("$id 의 layout_gravity 에 top 이 없다(현재: $gravity)", "top" in gravity)
        }
    }

    // 세로 레이아웃만 고친 상태에서 가로/다른 크기 변형이 생기면 FragmentHomeBinding 필드가 조용히 @Nullable로 약해지거나 뷰 클래스가
    // 뭉개진다. 변형을 만들 땐 이 테스트를 지우지 말고 SearchLayoutConsistencyTest 같은 id/클래스 일치 테스트로 바꿔야 한다.
    @Test
    fun `no resource-qualified variant of fragment_home exists yet`() {
        val variants = File("src/main/res").listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("layout") && it.name != "layout" }
            .filter { File(it, "fragment_home.xml").exists() }
            .map { it.name }

        assertTrue(
            "fragment_home.xml 변형이 생겼다: $variants — 세로와 id/뷰 클래스가 같은지 검증하는 일치 테스트를 먼저 추가할 것",
            variants.isEmpty()
        )
    }

    // ---- 규칙 감도: 일부러 망가뜨린 XML 사본(문자열 변형)에서 규칙이 실제로 위반을 잡아내는지 확인한다 ----

    @Test
    fun `sensitivity - hazard rule flags every unsafe kind of descendant`() {
        assertTrue("기준 XML 은 위험 요소가 없어야 한다", home.unboundedHeightHazards().isEmpty())
        val recycler = "androidx.recyclerview.widget.RecyclerView"
        val rvId = "android:id=\"@+id/rv_extra\""
        // 이름 -> (기대하는 규칙 표식, 끼워 넣을 XML). 표식까지 확인해야 다른 규칙에 우연히 걸려 통과하는 일이 없다.
        val unsafeSnippets = mapOf(
            "ComposeView" to ("(a)" to node("androidx.compose.ui.platform.ComposeView")),
            "NestedScrollView" to ("(b)" to node("androidx.core.widget.NestedScrollView")),
            "ScrollView" to ("(b)" to node("ScrollView")),
            "orientation 없는 RecyclerView" to ("(c)" to node(recycler, extra = rvId)),
            "세로 RecyclerView" to
                ("(c)" to node(recycler, extra = "$rvId android:orientation=\"vertical\"")),
            "match_parent 높이 뷰" to ("(d)" to node("View", height = "match_parent")),
            "높이 없는 include" to ("(e)" to "<include layout=\"@layout/layout_error\" />")
        )

        for ((name, expected) in unsafeSnippets) {
            val (marker, snippet) = expected
            val hazards = parseXml(homeXml.insertIntoSectionAppBar(snippet)).unboundedHeightHazards()
            assertTrue("$name 를 넣었는데 $marker 규칙으로 잡히지 않았다: $hazards", hazards.any { marker in it })
        }
    }

    @Test
    fun `sensitivity - hazard rule accepts horizontal recycler views and sized includes`() {
        val safeSnippets = mapOf(
            "가로 RecyclerView" to node(
                "androidx.recyclerview.widget.RecyclerView",
                extra = "android:id=\"@+id/rv_extra\" android:orientation=\"horizontal\""
            ),
            "높이가 있는 include" to node("include", extra = "layout=\"@layout/layout_error\"")
        )

        for ((name, snippet) in safeSnippets) {
            val hazards = parseXml(homeXml.insertIntoSectionAppBar(snippet)).unboundedHeightHazards()
            assertTrue("$name 는 안전한데 위험으로 잡혔다: $hazards", hazards.isEmpty())
        }
    }

    @Test
    fun `sensitivity - hazard rule is not defeated by a vertical recycler view that borrows an allowed id`() {
        // 허용 목록은 id 기준이므로, 허용 id 가 아닌 새 id 의 RecyclerView 는 그대로 걸려야 한다
        val copyId = "android:id=\"@+id/rv_box_office_copy\""
        val borrowed = node("androidx.recyclerview.widget.RecyclerView", extra = copyId)

        val hazards = parseXml(homeXml.insertIntoSectionAppBar(borrowed)).unboundedHeightHazards()

        assertTrue("허용 id 와 비슷한 새 id 가 허용 목록을 통과했다", hazards.isNotEmpty())
    }

    @Test
    fun `sensitivity - scroll order rule flags a fixed child placed before a scroll flagged child`() {
        // 첫 섹션(box_office_section)의 scroll 플래그를 지우면 고정 자식 뒤에 scroll 자식(watch_history_section)이 온다
        val scrollFlag = "app:layout_scrollFlags=\"scroll\""
        val firstSectionFixed = homeXml.replaceFirst(scrollFlag, "")
        assertTrue("변형이 적용되지 않았다", firstSectionFixed != homeXml)

        val violations = parseXml(firstSectionFixed).byId("app_bar_home_sections").scrollOrderViolations()

        assertTrue("고정 자식 뒤의 scroll 자식을 못 잡았다", violations.isNotEmpty())
    }

    @Test
    fun `sensitivity - scroll order rule accepts a fixed child placed after the scroll flagged ones`() {
        val scrollFlag = "app:layout_scrollFlags=\"scroll\""
        val lastSectionFixed = homeXml.removeSuffixOccurrence(scrollFlag)
        assertTrue("변형이 적용되지 않았다", lastSectionFixed != homeXml)

        val violations = parseXml(lastSectionFixed).byId("app_bar_home_sections").scrollOrderViolations()

        assertTrue("고정 자식이 맨 뒤인데 위반으로 잡혔다: $violations", violations.isEmpty())
    }

    @Test
    fun `sensitivity - id set rule flags an extra id and a missing id`() {
        assertTrue("기준 XML 의 id 집합은 기대와 같아야 한다", home.idSetChanges().isEmpty())

        val withExtraId = parseXml(homeXml.insertIntoSectionAppBar(node("View", extra = "android:id=\"@+id/extra\"")))
        val withoutFabId = parseXml(homeXml.replace("android:id=\"@+id/fab_scroll_top\"", ""))

        assertTrue("모르는 id 가 늘어난 걸 못 잡았다", withExtraId.idSetChanges().isNotEmpty())
        assertTrue("id 가 사라진 걸 못 잡았다", withoutFabId.idSetChanges().isNotEmpty())
    }

    // ---- 규칙 ----

    // (id, 태그명[, include 대상]) 집합이 "기존 19개 + 신규 2개"와 정확히 같은지. 다르면 사람이 읽을 변경 목록을 돌려준다.
    private fun Document.idSetChanges(): List<String> {
        val expected = (LEGACY_ID_ENTRIES + NEW_ID_ENTRIES).sorted()
        val actual = idEntries()
        return actual.missingFrom(expected).map { "예상에 없는 id: $it" } +
            expected.missingFrom(actual).map { "사라진 id: $it" }
    }

    private fun Document.unboundedHeightHazards(): List<String> =
        byId("app_bar_home_sections").descendants().flatMap { view ->
            view.hazardsOf().map { "${view.label()}: $it" }
        }

    private fun Element.hazardsOf(): List<String> {
        val kind = tagName.substringAfterLast('.')
        val height = androidAttr("layout_height")
        return listOfNotNull(
            "(a) ComposeView".takeIf { kind == "ComposeView" },
            "(b) 세로 스크롤 뷰".takeIf { kind in VERTICAL_SCROLL_VIEWS },
            "(c) 세로 RecyclerView".takeIf { isVerticalOrUnknownRecyclerView(kind) },
            "(d) layout_height=$height".takeIf { height in MATCH_PARENT_VALUES },
            "(e) layout_height 없는 include".takeIf { kind == "include" && height.isEmpty() }
        )
    }

    private fun Element.isVerticalOrUnknownRecyclerView(kind: String): Boolean {
        val isAllowedById = viewId() in HORIZONTAL_BY_CODE_RECYCLER_IDS
        val isHorizontalInXml = androidAttr("orientation") == "horizontal"
        return kind == "RecyclerView" && !isAllowedById && !isHorizontalInXml
    }

    // scroll 플래그가 없는(고정) 자식이 나온 뒤에 scroll 플래그 자식이 오면 위반
    private fun Element.scrollOrderViolations(): List<String> {
        val children = childElements()
        val firstFixed = children.indexOfFirst { !it.hasScrollFlag() }
        if (firstFixed < 0) return emptyList()
        val fixedLabel = children[firstFixed].label()
        return children.drop(firstFixed + 1)
            .filter { it.hasScrollFlag() }
            .map { "${it.label()} 가 고정 자식 $fixedLabel 뒤에 있다" }
    }

    private fun Element.hasScrollFlag(): Boolean {
        val flags = appAttr("layout_scrollFlags").split('|').map { it.trim() }
        return "scroll" in flags
    }

    private fun Element.label(): String = "${viewId().ifEmpty { "<id 없음>" }} <$tagName>"

    // ---- 파싱/DOM 도우미 ----

    private fun readXml(path: String): String {
        val file = File(path)
        // assumeTrue로 건너뛰면 파일이 없을 때 조용히 통과하므로 실패시킨다(Gradle 테스트 workingDir = app/)
        assertTrue("$path 파일이 없다 (workingDir=${File("").absolutePath})", file.exists())
        return file.readText()
    }

    private fun parseXml(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            isXIncludeAware = false
            isExpandEntityReferences = false
            isNamespaceAware = true
        }
        return factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
    }

    // 안쪽 앱바의 닫는 태그 바로 앞에 snippet 을 끼워 넣은 XML 사본(원본 파일은 그대로)
    private fun String.insertIntoSectionAppBar(snippet: String): String {
        val idAt = indexOf("@+id/app_bar_home_sections")
        assertTrue("fragment_home.xml 에서 app_bar_home_sections 를 못 찾았다", idAt >= 0)
        val closeAt = indexOf(APP_BAR_CLOSE_TAG, idAt)
        assertTrue("app_bar_home_sections 의 닫는 태그를 못 찾았다", closeAt > idAt)
        return substring(0, closeAt) + snippet + substring(closeAt)
    }

    // 마지막으로 나오는 target 만 지운 사본
    private fun String.removeSuffixOccurrence(target: String): String {
        val at = lastIndexOf(target)
        return if (at < 0) this else removeRange(at, at + target.length)
    }

    private fun node(tag: String, height: String = "wrap_content", extra: String = ""): String =
        "<$tag android:layout_width=\"match_parent\" android:layout_height=\"$height\" $extra />"

    // (id, 태그명[, include 대상]) 목록 — 맵이 아니라 정렬된 목록이라 중복 id도 차이로 잡힌다
    private fun Document.idEntries(): List<String> = elements().mapNotNull { element ->
        val id = element.viewId()
        if (id.isEmpty()) return@mapNotNull null
        val target = if (element.tagName == "include") " ${element.getAttribute("layout")}" else ""
        "$id <${element.tagName}$target>"
    }.sorted()

    private fun Document.elements(): List<Element> {
        val nodes = getElementsByTagName("*")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Document.byId(id: String): Element {
        val matches = elements().filter { it.viewId() == id }
        assertEquals("id '$id' 는 정확히 한 번 선언돼야 한다(현재 ${matches.size}번)", 1, matches.size)
        return matches.single()
    }

    private fun Element.viewId(): String = androidAttr("id").removePrefix("@+id/").removePrefix("@id/")

    private fun Element.androidAttr(name: String): String = getAttributeNS(ANDROID_NS, name)

    private fun Element.appAttr(name: String): String = getAttributeNS(APP_NS, name)

    private fun Element.parentElement(): Element? = parentNode as? Element

    private fun Element.ancestors(): List<Element> = generateSequence(parentElement()) { it.parentElement() }.toList()

    private fun Element.childElements(): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun Element.descendants(): List<Element> {
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
        const val APP_NS = "http://schemas.android.com/apk/res-auto"
        const val COORDINATOR_CLASS = "androidx.coordinatorlayout.widget.CoordinatorLayout"
        const val APP_BAR_CLASS = "com.google.android.material.appbar.AppBarLayout"
        const val APP_BAR_CLOSE_TAG = "</com.google.android.material.appbar.AppBarLayout>"
        val MATCH_PARENT_VALUES = setOf("match_parent", "fill_parent")

        // 세로 스크롤을 만드는 스크롤 뷰. HorizontalScrollView 는 높이를 내용으로 정하므로 제외한다.
        val VERTICAL_SCROLL_VIEWS = setOf("ScrollView", "NestedScrollView")

        // 허용 목록(최소): 섹션 앱바 안의 RecyclerView 중 XML 에 orientation 이 없어도 되는 것. 두 목록 모두 XML 에는 layoutManager/
        // orientation 이 없고 HomeFragment 가 코드에서 가로 LinearLayoutManager(HORIZONTAL)를 지정한다(setupBoxOffice/
        // setupWatchHistory). 가로 목록은 높이를 항목 높이로 정하므로 UNSPECIFIED 측정에서도 안전하다. 코드 지정은 XML 만으로 알 수
        // 없어 id 로 허용하는 것이므로, HomeFragment 쪽을 세로로 바꾸면 이 테스트는 못 잡는다 — 그 변경 때 함께 볼 것.
        val HORIZONTAL_BY_CODE_RECYCLER_IDS = setOf("rv_box_office", "rv_watch_history")

        // 구조 변경 전(base 1873438) fragment_home.xml에서 그대로 뽑은 (id, 태그[, include 대상]) 목록
        val LEGACY_ID_ENTRIES = listOf(
            "toolbar <com.google.android.material.appbar.MaterialToolbar>",
            "tab_layout <com.google.android.material.tabs.TabLayout>",
            "box_office_section <LinearLayout>",
            "tv_box_office_label <TextView>",
            "progress_box_office <ProgressBar>",
            "chip_group_box_office_period <com.google.android.material.chip.ChipGroup>",
            "chip_box_office_daily <com.google.android.material.chip.Chip>",
            "chip_box_office_weekly <com.google.android.material.chip.Chip>",
            "rv_box_office <androidx.recyclerview.widget.RecyclerView>",
            "tv_box_office_error <TextView>",
            "watch_history_section <LinearLayout>",
            "tv_watch_history_label <TextView>",
            "rv_watch_history <androidx.recyclerview.widget.RecyclerView>",
            "swipe_refresh <androidx.swiperefreshlayout.widget.SwipeRefreshLayout>",
            "rv_movies <androidx.recyclerview.widget.RecyclerView>",
            "shimmer_view <include @layout/layout_shimmer_grid>",
            "error_view <include @layout/layout_error>",
            "empty_view <include @layout/layout_empty_state>",
            "fab_scroll_top <com.google.android.material.floatingactionbutton.FloatingActionButton>"
        ).sorted()

        // 이번 구조 변경에서 새로 생긴 id 2개
        val NEW_ID_ENTRIES = listOf(
            "home_sections_coordinator <$COORDINATOR_CLASS>",
            "app_bar_home_sections <$APP_BAR_CLASS>"
        )
    }
}
