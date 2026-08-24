package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.KoreanRatingCacheDao
import com.choo.moviefinder.data.local.entity.KoreanRatingCacheEntity
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
import org.junit.Assert.assertNull
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
    private lateinit var cacheDao: KoreanRatingCacheDao
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
        cacheDao = mockk()
        // 파서는 실물 인스턴스를 사용해 파싱 통합까지 검증한다 (예외 케이스만 개별적으로 mock 파서 사용)
        repository = KoreanRatingRepositoryImpl(apiService, KmrbRatingXmlParser(), cacheDao)
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
    fun `searchRatings throws Unknown on unrecognized error code instead of returning empty`() = runTest {
        // "등급 없음"(정상)과 "알 수 없는 에러"를 구분하지 않고 emptyList()로 뭉개면
        // getRatingForMovie()가 일시적 서버 오류를 영구 negative 캐싱하게 된다.
        val xml = "<response><body><resultCode>99</resultCode><resultMsg>UNKNOWN</resultMsg></body></response>"
        coEvery { apiService.searchMovieRating("에러 영화", 1, 10) } returns xmlResponseBody(xml)

        try {
            repository.searchRatings("에러 영화")
            fail("Expected DomainException.Unknown")
        } catch (e: DomainException.Unknown) {
            assertTrue(e.cause?.message?.contains("99") == true)
        }
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
        val repoWithMockParser = KoreanRatingRepositoryImpl(apiService, mockParser, cacheDao)
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

    // --- getRatingForMovie (캐시 우선 조회) ---

    @Test
    fun `getRatingForMovie returns cached rating and never calls api on cache hit with found=true`() = runTest {
        val cached = KoreanRatingCacheEntity(
            movieTitle = "캐시된 영화",
            found = true,
            gradeName = "15세이상관람가",
            matchedTitle = "캐시된 영화",
            productionCompany = "제작사",
            directorName = "감독",
            productionYear = "2024",
            ratingReason = "사유",
            cachedAt = 1000L
        )
        coEvery { cacheDao.find("캐시된 영화") } returns cached

        val result = repository.getRatingForMovie("캐시된 영화")

        assertEquals("15세이상관람가", result?.gradeName)
        coVerify(exactly = 0) { apiService.searchMovieRating(any(), any(), any()) }
    }

    @Test
    fun `getRatingForMovie returns null and never calls api on fresh cache hit with found=false`() = runTest {
        val cached = KoreanRatingCacheEntity(
            movieTitle = "없는 영화",
            found = false,
            gradeName = null,
            matchedTitle = null,
            productionCompany = null,
            directorName = null,
            productionYear = null,
            ratingReason = null,
            cachedAt = System.currentTimeMillis()
        )
        coEvery { cacheDao.find("없는 영화") } returns cached

        val result = repository.getRatingForMovie("없는 영화")

        assertNull(result)
        coVerify(exactly = 0) { apiService.searchMovieRating(any(), any(), any()) }
    }

    @Test
    fun `getRatingForMovie re-queries network when found=false cache entry is older than TTL`() = runTest {
        // KMRB에 아직 미등록(개봉 전 등)일 수 있어 negative 캐시는 24시간 후 만료돼야 한다.
        val staleCached = KoreanRatingCacheEntity(
            movieTitle = "개봉예정작",
            found = false,
            gradeName = null,
            matchedTitle = null,
            productionCompany = null,
            directorName = null,
            productionYear = null,
            ratingReason = null,
            cachedAt = System.currentTimeMillis() - java.util.concurrent.TimeUnit.HOURS.toMillis(25)
        )
        coEvery { cacheDao.find("개봉예정작") } returns staleCached
        coEvery { apiService.searchMovieRating("개봉예정작", 1, 10) } returns xmlResponseBody(singleItemXml)
        coEvery { cacheDao.upsert(any()) } returns Unit

        val result = repository.getRatingForMovie("개봉예정작")

        coVerify(exactly = 1) { apiService.searchMovieRating("개봉예정작", 1, 10) }
        assertNull(result) // singleItemXml의 title("테스트 영화")이 "개봉예정작"과 일치하지 않아 미매치
    }

    @Test
    fun `getRatingForMovie calls network and caches found=true on cache miss with match`() = runTest {
        coEvery { cacheDao.find("테스트 영화") } returns null
        coEvery { apiService.searchMovieRating("테스트 영화", 1, 10) } returns xmlResponseBody(singleItemXml)
        coEvery { cacheDao.upsert(any()) } returns Unit

        val result = repository.getRatingForMovie("테스트 영화")

        assertEquals("15세이상관람가", result?.gradeName)
        coVerify(exactly = 1) {
            cacheDao.upsert(
                match { entity -> entity.movieTitle == "테스트 영화" && entity.found }
            )
        }
    }

    @Test
    fun `getRatingForMovie caches found=false on cache miss with no matching candidate`() = runTest {
        coEvery { cacheDao.find("일치안함") } returns null
        val xml = """
            <response><body><items>
                <item><useTitle>전혀 다른 제목</useTitle></item>
            </items></body></response>
        """.trimIndent()
        coEvery { apiService.searchMovieRating("일치안함", 1, 10) } returns xmlResponseBody(xml)
        coEvery { cacheDao.upsert(any()) } returns Unit

        val result = repository.getRatingForMovie("일치안함")

        assertNull(result)
        coVerify(exactly = 1) {
            cacheDao.upsert(
                match { entity -> entity.movieTitle == "일치안함" && !entity.found }
            )
        }
    }

    @Test
    fun `getRatingForMovie does not call dao or api for blank title`() = runTest {
        val result = repository.getRatingForMovie("   ")

        assertNull(result)
        coVerify(exactly = 0) { cacheDao.find(any()) }
        coVerify(exactly = 0) { apiService.searchMovieRating(any(), any(), any()) }
    }

    @Test
    fun `getRatingForMovie does not cache when network call throws`() = runTest {
        coEvery { cacheDao.find("네트워크실패") } returns null
        coEvery { apiService.searchMovieRating("네트워크실패", 1, 10) } throws IOException("no connection")

        try {
            repository.getRatingForMovie("네트워크실패")
            fail("Expected DomainException.NetworkError")
        } catch (e: DomainException.NetworkError) {
            assertTrue(e.cause is IOException)
        }

        coVerify(exactly = 0) { cacheDao.upsert(any()) }
    }
}
