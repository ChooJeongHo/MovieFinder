package com.choo.moviefinder.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream

class KmrbRatingXmlParserTest {

    private val parser = KmrbRatingXmlParser()

    private fun parse(xml: String) = parser.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

    @Test
    fun `parse maps single item's 6 fields correctly`() {
        val xml = """
            <response>
                <body>
                    <items>
                        <item>
                            <gradeName>15세이상관람가</gradeName>
                            <useTitle>테스트 영화</useTitle>
                            <prodcName>테스트 제작사</prodcName>
                            <directorName>테스트 감독</directorName>
                            <prodYear>2024</prodYear>
                            <majorOpinNarrnCont>폭력성이 있음</majorOpinNarrnCont>
                        </item>
                    </items>
                </body>
            </response>
        """.trimIndent()

        val result = parse(xml)

        assertEquals(1, result.items.size)
        val item = result.items[0]
        assertEquals("15세이상관람가", item.gradeName)
        assertEquals("테스트 영화", item.useTitle)
        assertEquals("테스트 제작사", item.prodcName)
        assertEquals("테스트 감독", item.directorName)
        assertEquals("2024", item.prodYear)
        assertEquals("폭력성이 있음", item.majorOpinNarrnCont)
    }

    @Test
    fun `parse keeps two items in original order`() {
        val xml = """
            <response><body><items>
                <item><useTitle>첫번째 영화</useTitle></item>
                <item><useTitle>두번째 영화</useTitle></item>
            </items></body></response>
        """.trimIndent()

        val result = parse(xml)

        assertEquals(2, result.items.size)
        assertEquals("첫번째 영화", result.items[0].useTitle)
        assertEquals("두번째 영화", result.items[1].useTitle)
    }

    @Test
    fun `parse fills missing fields with empty string default`() {
        val xml = """
            <response><body><items>
                <item><useTitle>부분 정보만 있는 영화</useTitle></item>
            </items></body></response>
        """.trimIndent()

        val result = parse(xml)

        val item = result.items[0]
        assertEquals("부분 정보만 있는 영화", item.useTitle)
        assertEquals("", item.gradeName)
        assertEquals("", item.prodcName)
        assertEquals("", item.directorName)
        assertEquals("", item.prodYear)
        assertEquals("", item.majorOpinNarrnCont)
    }

    @Test
    fun `parse ignores unknown interspersed tags`() {
        val xml = """
            <response><body><items>
                <item>
                    <someUnknownTag>무시되어야 함</someUnknownTag>
                    <useTitle>테스트 영화</useTitle>
                    <anotherUnknown><nested>깊은 태그</nested></anotherUnknown>
                    <gradeName>전체관람가</gradeName>
                </item>
            </items></body></response>
        """.trimIndent()

        val result = parse(xml)

        assertEquals(1, result.items.size)
        assertEquals("테스트 영화", result.items[0].useTitle)
        assertEquals("전체관람가", result.items[0].gradeName)
    }

    @Test
    fun `parse returns empty list without exception when items tag is absent`() {
        val xml = """
            <response><body><resultCode>00</resultCode></body></response>
        """.trimIndent()

        val result = parse(xml)

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `parse collects top-level resultCode and resultMsg`() {
        val xml = """
            <response><body>
                <resultCode>00</resultCode>
                <resultMsg>NORMAL SERVICE</resultMsg>
            </body></response>
        """.trimIndent()

        val result = parse(xml)

        assertEquals("00", result.resultCode)
        assertEquals("NORMAL SERVICE", result.resultMsg)
    }

    @Test
    fun `parse leaves resultCode null when tag is absent`() {
        val xml = """
            <response><body><items>
                <item><useTitle>테스트 영화</useTitle></item>
            </items></body></response>
        """.trimIndent()

        val result = parse(xml)

        assertNull(result.resultCode)
    }

    @Test
    fun `parse accumulates long text spanning multiple characters callbacks`() {
        val longText = "가".repeat(20_000)
        val xml = """
            <response><body><items>
                <item><majorOpinNarrnCont>$longText</majorOpinNarrnCont></item>
            </items></body></response>
        """.trimIndent()

        val result = parse(xml)

        assertEquals(20_000, result.items[0].majorOpinNarrnCont.length)
        assertEquals(longText, result.items[0].majorOpinNarrnCont)
    }

    @Test
    fun `parse trims leading and trailing whitespace and newlines`() {
        val xml = """
            <response><body><items>
                <item><useTitle>
                    공백이 있는 제목
                </useTitle></item>
            </items></body></response>
        """.trimIndent()

        val result = parse(xml)

        assertEquals("공백이 있는 제목", result.items[0].useTitle)
    }

    @Test
    fun `parse preserves Korean UTF-8 text without corruption`() {
        val xml = """
            <response><body><items>
                <item><useTitle>기생충: 감독판</useTitle></item>
            </items></body></response>
        """.trimIndent()

        val result = parse(xml)

        assertEquals("기생충: 감독판", result.items[0].useTitle)
    }

    @Test(expected = SAXException::class)
    fun `parse throws SAXException for malformed XML with unclosed tag`() {
        val xml = """
            <response><body><items>
                <item><useTitle>닫히지 않은 태그
            </items></body></response>
        """.trimIndent()

        parse(xml)
    }

    @Test(expected = SAXException::class)
    fun `parse rejects DOCTYPE declarations to prevent XXE`() {
        val xml = """
            <?xml version="1.0"?>
            <!DOCTYPE response [
                <!ENTITY xxe "injected-secret-value">
            ]>
            <response><body><items>
                <item><useTitle>&xxe;</useTitle></item>
            </items></body></response>
        """.trimIndent()

        parse(xml)
    }
}
