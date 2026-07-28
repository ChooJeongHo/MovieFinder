package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.KoficApiService
import com.choo.moviefinder.data.remote.dto.KoficBoxOfficeResponse
import com.choo.moviefinder.data.remote.dto.KoficBoxOfficeResult
import com.choo.moviefinder.data.remote.dto.KoficBoxOfficeItemDto
import com.choo.moviefinder.data.remote.dto.KoficFaultInfo
import com.choo.moviefinder.domain.model.BoxOfficeWeekType
import com.choo.moviefinder.domain.model.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BoxOfficeRepositoryImplTest {

    private lateinit var apiService: KoficApiService
    private lateinit var repository: BoxOfficeRepositoryImpl

    private val testDto = KoficBoxOfficeItemDto(
        rank = "1",
        rankInten = "2",
        rankOldAndNew = "OLD",
        movieCd = "20240001",
        movieNm = "테스트 영화",
        openDt = "2024-01-01",
        audiCnt = "12345",
        audiAcc = "678900",
        salesAmt = "999000000",
        scrnCnt = "500"
    )

    @Before
    fun setUp() {
        apiService = mockk()
        repository = BoxOfficeRepositoryImpl(apiService)
    }

    @Test
    fun `getDailyBoxOffice returns mapped domain list`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240101") } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult(listOf(testDto)))

        val result = repository.getDailyBoxOffice("20240101")

        assertEquals(1, result.size)
        val boxOffice = result[0]
        assertEquals(1, boxOffice.rank)
        assertEquals(2, boxOffice.rankChange)
        assertEquals(false, boxOffice.isNewEntry)
        assertEquals("20240001", boxOffice.movieCode)
        assertEquals("테스트 영화", boxOffice.movieName)
        assertEquals(12345L, boxOffice.audienceCount)
        assertEquals(678900L, boxOffice.audienceAccumulate)
        assertEquals(999000000L, boxOffice.salesAmount)
        assertEquals(500, boxOffice.screenCount)
    }

    @Test
    fun `getDailyBoxOffice maps NEW rankOldAndNew to isNewEntry true`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240101") } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult(listOf(testDto.copy(rankOldAndNew = "NEW"))))

        val result = repository.getDailyBoxOffice("20240101")

        assertTrue(result[0].isNewEntry)
    }

    @Test
    fun `getDailyBoxOffice delegates to apiService with given targetDate`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240315") } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult(emptyList()))

        repository.getDailyBoxOffice("20240315")

        coVerify(exactly = 1) { apiService.getDailyBoxOffice("20240315") }
    }

    @Test
    fun `getDailyBoxOffice returns empty list when no data available`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240101") } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult(emptyList()))

        val result = repository.getDailyBoxOffice("20240101")

        assertTrue(result.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getDailyBoxOffice blank targetDate throws`() = runTest {
        repository.getDailyBoxOffice("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getDailyBoxOffice non-8-digit targetDate throws`() = runTest {
        repository.getDailyBoxOffice("2024-01-01")
    }

    @Test
    fun `getDailyBoxOffice handles unparsable numeric fields with default zero`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240101") } returns
            KoficBoxOfficeResponse(
                KoficBoxOfficeResult(
                    listOf(testDto.copy(rank = "N/A", audiCnt = "N/A", scrnCnt = "N/A"))
                )
            )

        val result = repository.getDailyBoxOffice("20240101")

        assertEquals(0, result[0].rank)
        assertEquals(0L, result[0].audienceCount)
        assertEquals(0, result[0].screenCount)
    }

    // KOFIC은 key 오류를 HTTP 401이 아니라 200 OK + faultInfo(errorCode="320001")로 반환한다.
    // 실기기에서 빈 KOFIC_API_KEY로 실제 호출해보고 발견한 회귀 케이스 — 반드시 Unauthorized로 매핑돼야 한다.
    @Test
    fun `getDailyBoxOffice maps auth faultInfo (errorCode 32x) to Unauthorized`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240101") } returns
            KoficBoxOfficeResponse(
                boxOfficeResult = null,
                faultInfo = KoficFaultInfo(message = "인증키가 유효하지 않습니다.", errorCode = "320001")
            )

        try {
            repository.getDailyBoxOffice("20240101")
            org.junit.Assert.fail("Expected DomainException.Unauthorized")
        } catch (e: DomainException.Unauthorized) {
            assertEquals("인증키가 유효하지 않습니다.", e.cause?.message)
        }
    }

    @Test
    fun `getDailyBoxOffice maps non-auth faultInfo to ServerError`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240101") } returns
            KoficBoxOfficeResponse(
                boxOfficeResult = null,
                faultInfo = KoficFaultInfo(message = "필수 파라미터가 누락되었습니다.", errorCode = "310000")
            )

        try {
            repository.getDailyBoxOffice("20240101")
            org.junit.Assert.fail("Expected DomainException.ServerError")
        } catch (e: DomainException.ServerError) {
            assertEquals("필수 파라미터가 누락되었습니다.", e.cause?.message)
        }
    }

    @Test
    fun `getDailyBoxOffice maps missing boxOfficeResult and faultInfo to ServerError`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240101") } returns
            KoficBoxOfficeResponse(boxOfficeResult = null, faultInfo = null)

        try {
            repository.getDailyBoxOffice("20240101")
            org.junit.Assert.fail("Expected DomainException.ServerError")
        } catch (e: DomainException.ServerError) {
            assertTrue(e.cause is IllegalStateException)
        }
    }

    // ── 주간 박스오피스 ────────────────────────────────────

    // 일별/주간이 같은 KoficBoxOfficeResult를 공유하므로, 두 배열을 서로 다른 값으로 동시에 채워
    // 각 메서드가 자기 필드만 읽는지 교차 검증한다. 한쪽만 채우면 필드 오독이 emptyList()로 가려진다.
    @Test
    fun `getDailyBoxOffice reads dailyBoxOfficeList only when both lists are populated`() = runTest {
        coEvery { apiService.getDailyBoxOffice("20240101") } returns KoficBoxOfficeResponse(
            KoficBoxOfficeResult(
                dailyBoxOfficeList = listOf(testDto.copy(movieNm = "일별 영화", movieCd = "DAILY1")),
                weeklyBoxOfficeList = listOf(testDto.copy(movieNm = "주간 영화", movieCd = "WEEKLY1"))
            )
        )

        val result = repository.getDailyBoxOffice("20240101")

        assertEquals(1, result.size)
        assertEquals("일별 영화", result[0].movieName)
        assertEquals("DAILY1", result[0].movieCode)
    }

    @Test
    fun `getWeeklyBoxOffice reads weeklyBoxOfficeList only when both lists are populated`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "0", 10) } returns KoficBoxOfficeResponse(
            KoficBoxOfficeResult(
                dailyBoxOfficeList = listOf(testDto.copy(movieNm = "일별 영화", movieCd = "DAILY1")),
                weeklyBoxOfficeList = listOf(testDto.copy(movieNm = "주간 영화", movieCd = "WEEKLY1"))
            )
        )

        val result = repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEK)

        assertEquals(1, result.size)
        assertEquals("주간 영화", result[0].movieName)
        assertEquals("WEEKLY1", result[0].movieCode)
    }

    @Test
    fun `getWeeklyBoxOffice returns fully mapped domain list`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "0", 10) } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult(weeklyBoxOfficeList = listOf(testDto)))

        val result = repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEK)

        val boxOffice = result[0]
        assertEquals(1, boxOffice.rank)
        assertEquals(2, boxOffice.rankChange)
        assertEquals(false, boxOffice.isNewEntry)
        assertEquals("20240001", boxOffice.movieCode)
        assertEquals("테스트 영화", boxOffice.movieName)
        assertEquals(12345L, boxOffice.audienceCount)
        assertEquals(678900L, boxOffice.audienceAccumulate)
        assertEquals(999000000L, boxOffice.salesAmount)
        assertEquals(500, boxOffice.screenCount)
    }

    // KOFIC weekGb: 0=주간, 1=주말, 2=주중 (잘못된 값은 faultInfo 320105로 거부됨)
    @Test
    fun `getWeeklyBoxOffice maps WEEK to weekGb 0`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "0", 10) } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult())

        repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEK)

        coVerify(exactly = 1) { apiService.getWeeklyBoxOffice("20240101", "0", 10) }
    }

    @Test
    fun `getWeeklyBoxOffice maps WEEKEND to weekGb 1`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "1", 10) } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult())

        repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEKEND)

        coVerify(exactly = 1) { apiService.getWeeklyBoxOffice("20240101", "1", 10) }
    }

    @Test
    fun `getWeeklyBoxOffice maps WEEKDAY to weekGb 2`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "2", 10) } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult())

        repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEKDAY)

        coVerify(exactly = 1) { apiService.getWeeklyBoxOffice("20240101", "2", 10) }
    }

    @Test
    fun `getWeeklyBoxOffice returns empty list when no data available`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "0", 10) } returns
            KoficBoxOfficeResponse(KoficBoxOfficeResult(weeklyBoxOfficeList = emptyList()))

        val result = repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEK)

        assertTrue(result.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getWeeklyBoxOffice blank targetDate throws`() = runTest {
        repository.getWeeklyBoxOffice("", BoxOfficeWeekType.WEEK)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getWeeklyBoxOffice hyphenated targetDate throws`() = runTest {
        repository.getWeeklyBoxOffice("2024-01-01", BoxOfficeWeekType.WEEK)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getWeeklyBoxOffice 6-digit targetDate throws`() = runTest {
        repository.getWeeklyBoxOffice("202401", BoxOfficeWeekType.WEEK)
    }

    @Test
    fun `getWeeklyBoxOffice maps auth faultInfo (errorCode 32x) to Unauthorized`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "0", 10) } returns KoficBoxOfficeResponse(
            boxOfficeResult = null,
            faultInfo = KoficFaultInfo(message = "유효하지않은 키값입니다.", errorCode = "320010")
        )

        try {
            repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEK)
            org.junit.Assert.fail("Expected DomainException.Unauthorized")
        } catch (e: DomainException.Unauthorized) {
            assertEquals("유효하지않은 키값입니다.", e.cause?.message)
        }
    }

    @Test
    fun `getWeeklyBoxOffice maps non-auth faultInfo to ServerError`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "0", 10) } returns KoficBoxOfficeResponse(
            boxOfficeResult = null,
            faultInfo = KoficFaultInfo(message = "필수 파라미터가 누락되었습니다.", errorCode = "310000")
        )

        try {
            repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEK)
            org.junit.Assert.fail("Expected DomainException.ServerError")
        } catch (e: DomainException.ServerError) {
            assertEquals("필수 파라미터가 누락되었습니다.", e.cause?.message)
        }
    }

    @Test
    fun `getWeeklyBoxOffice maps missing boxOfficeResult and faultInfo to ServerError`() = runTest {
        coEvery { apiService.getWeeklyBoxOffice("20240101", "0", 10) } returns
            KoficBoxOfficeResponse(boxOfficeResult = null, faultInfo = null)

        try {
            repository.getWeeklyBoxOffice("20240101", BoxOfficeWeekType.WEEK)
            org.junit.Assert.fail("Expected DomainException.ServerError")
        } catch (e: DomainException.ServerError) {
            assertTrue(e.cause is IllegalStateException)
        }
    }
}
