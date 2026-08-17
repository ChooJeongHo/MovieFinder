package com.choo.moviefinder.data.remote.parser

import com.choo.moviefinder.data.remote.dto.KmrbRatingItemDto
import com.choo.moviefinder.data.remote.dto.KmrbSearchResult
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

// KMRB(영상물등급위원회) 응답의 전체 봉투 구조(response/body/items)와 에러코드 체계가 미확정이라,
// 정확한 중첩 깊이를 가정하지 않고 <item> 태그 + 알려진 6개 필드명만으로 방어적으로 파싱한다.
class KmrbRatingXmlParser @Inject constructor() {

    fun parse(source: InputStream): KmrbSearchResult {
        val factory = SAXParserFactory.newInstance().apply {
            // XXE 방어: 일부 feature는 Android 구현체가 지원하지 않을 수 있어 개별적으로 감싼다.
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { isXIncludeAware = false }
        }
        val handler = KmrbHandler()
        factory.newSAXParser().parse(source, handler)
        return handler.toResult()
    }

    private class KmrbHandler : DefaultHandler() {
        private val items = mutableListOf<KmrbRatingItemDto>()
        private var resultCode: String? = null
        private var resultMsg: String? = null

        private var inItem = false
        private var currentTag: String? = null
        private val textBuffer = StringBuilder()

        private var gradeName = ""
        private var useTitle = ""
        private var prodcName = ""
        private var directorName = ""
        private var prodYear = ""
        private var majorOpinNarrnCont = ""

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String,
            attributes: Attributes?
        ) {
            if (qName == "item") {
                inItem = true
                resetItemBuffer()
            }
            if (qName in KNOWN_TAGS) {
                currentTag = qName
                textBuffer.setLength(0)
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (currentTag != null) {
                textBuffer.append(ch, start, length)
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            if (qName in KNOWN_TAGS && currentTag == qName) {
                val value = textBuffer.toString().trim()
                if (inItem) {
                    assignItemField(qName, value)
                } else if (qName == "resultCode") {
                    resultCode = value
                } else if (qName == "resultMsg") {
                    resultMsg = value
                }
                currentTag = null
            }
            if (qName == "item") {
                items.add(
                    KmrbRatingItemDto(
                        gradeName = gradeName,
                        useTitle = useTitle,
                        prodcName = prodcName,
                        directorName = directorName,
                        prodYear = prodYear,
                        majorOpinNarrnCont = majorOpinNarrnCont
                    )
                )
                inItem = false
                resetItemBuffer()
            }
        }

        private fun assignItemField(tag: String, value: String) {
            when (tag) {
                "gradeName" -> gradeName = value
                "useTitle" -> useTitle = value
                "prodcName" -> prodcName = value
                "directorName" -> directorName = value
                "prodYear" -> prodYear = value
                "majorOpinNarrnCont" -> majorOpinNarrnCont = value
            }
        }

        private fun resetItemBuffer() {
            gradeName = ""
            useTitle = ""
            prodcName = ""
            directorName = ""
            prodYear = ""
            majorOpinNarrnCont = ""
        }

        fun toResult(): KmrbSearchResult = KmrbSearchResult(
            resultCode = resultCode,
            resultMsg = resultMsg,
            items = items
        )

        companion object {
            private val KNOWN_TAGS = setOf(
                "gradeName",
                "useTitle",
                "prodcName",
                "directorName",
                "prodYear",
                "majorOpinNarrnCont",
                "resultCode",
                "resultMsg"
            )
        }
    }
}
