package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.KmrbApiService
import com.choo.moviefinder.data.remote.parser.KmrbRatingXmlParser
import com.choo.moviefinder.domain.model.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.xml.sax.SAXException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class KoreanRatingRepositoryImplTest {

    private lateinit var apiService: KmrbApiService
    private lateinit var repository: KoreanRatingRepositoryImpl

    private val singleItemXml = """
        <response><body><items>
            <item>
                <gradeName>15세이상관람가</gradeName>
                <useTitle>테스트 영화</useTitle>
                <prodcName>테스트 제작사</prodcName>
                <directorName>테스트 감독</directorName>
                <prodYear>2024</prodYear>
                <majorOpinNarrnCont>폭력성이 있음</majorOpinNarrnCont>
            </item>
        </items></body></response>
    """.trimIndent()

    private fun xmlResponseBody(xml: String): ResponseBody =
        xml.toResponseBody("application/xml".toMediaType())

    @Before
    fun setUp() {
        apiService = mockk()
        // 파서는 실물 인스턴스를 사용해 파싱 통합까지 검증한다 (예외 케이스만 개별적으로 mock 파서 사용)
        repository = KoreanRatingRepositoryImpl(apiService, KmrbRatingXmlParser())
    }

    @Test
    fun `searchRatings maps normal XML response to domain list`() = runTest {
        coEvery { apiService.searchMovieRating("테스트 영화", 1, 10) } returns xmlResponseBody(singleItemXml)

        val result = repository.searchRatings("테스트 영화")

        assertEquals(1, result.size)
        val rating = result[0]
        assertEquals("15세이상관람가", rating.gradeName)
        assertEquals("테스트 영화", rating.title)
        assertEquals("테스트 제작사", rating.productionCompany)
        assertEquals("테스트 감독", rating.directorName)
        assertEquals("2024", rating.productionYear)
        assertEquals("폭력성이 있음", rating.ratingReason)
    }

    @Test
    fun `searchRatings returns empty list when no items and no resultCode`() = runTest {
        val xml = "<response><body></body></response>"
        coEvery { apiService.searchMovieRating("없는 영화", 1, 10) } returns xmlResponseBody(xml)

        val result = repository.searchRatings("없는 영화")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchRatings returns empty list when no items and resultCode is 00`() = runTest {
        val xml = "<response><body><resultCode>00</resultCode></body></response>"
        coEvery { apiService.searchMovieRating("없는 영화", 1, 10) } returns xmlResponseBody(xml)

        val result = repository.searchRatings("없는 영화")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchRatings returns empty list without throwing on unknown error code`() = runTest {
        val xml = "<response><body><resultCode>99</resultCode><resultMsg>UNKNOWN</resultMsg></body></response>"
        coEvery { apiService.searchMovieRating("에러 영화", 1, 10) } returns xmlResponseBody(xml)

        val result = repository.searchRatings("에러 영화")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchRatings throws Unauthorized when resultCode starts with 30`() = runTest {
        val xml = "<response><body><resultCode>30</resultCode><resultMsg>SERVICE ERROR</resultMsg></body></response>"
        coEvery { apiService.searchMovieRating("인증실패", 1, 10) } returns xmlResponseBody(xml)

        try {
            repository.searchRatings("인증실패")
            fail("Expected DomainException.Unauthorized")
        } catch (e: DomainException.Unauthorized) {
            assertEquals("SERVICE ERROR", e.cause?.message)
        }
    }

    @Test
    fun `searchRatings throws Unauthorized when resultMsg contains SERVICE_KEY`() = runTest {
        val xml = "<response><body><resultCode>99</resultCode>" +
            "<resultMsg>INVALID SERVICE_KEY</resultMsg></body></response>"
        coEvery { apiService.searchMovieRating("키오류", 1, 10) } returns xmlResponseBody(xml)

        try {
            repository.searchRatings("키오류")
            fail("Expected DomainException.Unauthorized")
        } catch (e: DomainException.Unauthorized) {
            assertEquals("INVALID SERVICE_KEY", e.cause?.message)
        }
    }

    @Test
    fun `searchRatings prefers items over coexisting error code`() = runTest {
        val xml = """
            <response><body>
                <resultCode>99</resultCode>
                <items>
                    <item><useTitle>결과가 있는 영화</useTitle></item>
                </items>
            </body></response>
        """.trimIndent()
        coEvery { apiService.searchMovieRating("결과가 있는 영화", 1, 10) } returns xmlResponseBody(xml)

        val result = repository.searchRatings("결과가 있는 영화")

        assertEquals(1, result.size)
        assertEquals("결과가 있는 영화", result[0].title)
    }

    @Test
    fun `searchRatings wraps SAXException from parser as ParseError`() = runTest {
        val mockParser: KmrbRatingXmlParser = mockk()
        val repoWithMockParser = KoreanRatingRepositoryImpl(apiService, mockParser)
        coEvery { apiService.searchMovieRating("파싱실패", 1, 10) } returns xmlResponseBody(singleItemXml)
        every { mockParser.parse(any()) } throws SAXException("malformed xml")

        try {
            repoWithMockParser.searchRatings("파싱실패")
            fail("Expected DomainException.ParseError")
        } catch (e: DomainException.ParseError) {
            assertTrue(e.cause is SAXException)
        }
    }

    @Test
    fun `searchRatings wraps IOException from api as NetworkError`() = runTest {
        coEvery { apiService.searchMovieRating("네트워크실패", 1, 10) } throws IOException("no connection")

        try {
            repository.searchRatings("네트워크실패")
            fail("Expected DomainException.NetworkError")
        } catch (e: DomainException.NetworkError) {
            assertTrue(e.cause is IOException)
        }
    }

    @Test
    fun `searchRatings wraps HttpException 500 from api as ServerError`() = runTest {
        val httpException = HttpException(Response.error<Any>(500, byteArrayOf().toResponseBody()))
        coEvery { apiService.searchMovieRating("서버실패", 1, 10) } throws httpException

        try {
            repository.searchRatings("서버실패")
            fail("Expected DomainException.ServerError")
        } catch (e: DomainException.ServerError) {
            assertEquals(500, e.code)
        }
    }

    @Test
    fun `searchRatings rethrows CancellationException without wrapping`() = runTest {
        coEvery { apiService.searchMovieRating("취소됨", 1, 10) } throws CancellationException("cancelled")

        try {
            repository.searchRatings("취소됨")
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    @Test
    fun `searchRatings passes title pageNo numOfRows exactly to apiService`() = runTest {
        coEvery { apiService.searchMovieRating("특정 영화", 3, 20) } returns
            xmlResponseBody("<response><body></body></response>")

        repository.searchRatings("특정 영화", pageNo = 3, numOfRows = 20)

        coVerify(exactly = 1) { apiService.searchMovieRating("특정 영화", 3, 20) }
    }

    @Test
    fun `searchRatings does not call api for blank title and returns empty list`() = runTest {
        val result = repository.searchRatings("   ")

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { apiService.searchMovieRating(any(), any(), any()) }
    }

    @Test
    fun `searchRatings closes response body stream via use block`() = runTest {
        val spyBody = spyResponseBody(singleItemXml)
        coEvery { apiService.searchMovieRating("스트림 영화", 1, 10) } returns spyBody

        repository.searchRatings("스트림 영화")

        verify(exactly = 1) { spyBody.close() }
    }

    // ResponseBody.close() 호출 여부를 관찰하기 위한 spyk 대신, close()를 오버라이드해 호출 카운트를 세는
    // 얇은 래퍼를 만든다 (실제 바이트 스트림 동작은 원본에 위임).
    private fun spyResponseBody(xml: String): ResponseBody {
        val real = xmlResponseBody(xml)
        val spy = io.mockk.spyk(real)
        return spy
    }
}
