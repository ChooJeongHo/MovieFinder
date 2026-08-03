package com.choo.moviefinder.presentation.widget

import com.choo.moviefinder.domain.model.BoxOffice
import com.choo.moviefinder.domain.model.BoxOfficeMovie
import com.choo.moviefinder.domain.model.Movie
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 위젯 스냅샷 매퍼/직렬화는 Android 프레임워크에 의존하지 않는 순수 로직이라 JVM 유닛 테스트 대상이다.
class BoxOfficeWidgetSnapshotTest {

    private fun boxOffice(rank: Int, movieName: String = "영화 $rank", audience: Long = rank * 1000L) =
        BoxOffice(
            rank = rank,
            rankChange = 0,
            isNewEntry = false,
            movieCode = "cd$rank",
            movieName = movieName,
            openDate = "2024-01-01",
            audienceCount = audience,
            audienceAccumulate = audience * 10,
            salesAmount = audience * 10_000,
            screenCount = 100
        )

    private fun movie(id: Int, title: String) =
        Movie(id, title, "/p.jpg", "/b.jpg", "overview", "2024-01-01", 7.5, 100)

    @Test
    fun `toWidgetSnapshot keeps only top 3 sorted by rank`() {
        val source = (1..10).shuffled().map { rank ->
            BoxOfficeMovie(boxOffice(rank), movie(rank * 100, "영화 $rank"))
        }

        val snapshot = source.toWidgetSnapshot(fetchedAtMillis = 1_234L)

        assertEquals(BoxOfficeWidget.TOP_COUNT, snapshot.items.size)
        assertEquals(listOf(1, 2, 3), snapshot.items.map { it.rank })
        assertEquals(listOf("영화 1", "영화 2", "영화 3"), snapshot.items.map { it.movieName })
        assertEquals(listOf(1000L, 2000L, 3000L), snapshot.items.map { it.audienceCount })
        assertEquals(1_234L, snapshot.fetchedAtMillis)
    }

    @Test
    fun `toWidgetSnapshot maps tmdb id and leaves null when unmatched`() {
        val source = listOf(
            BoxOfficeMovie(boxOffice(1), movie(550, "영화 1")),
            BoxOfficeMovie(boxOffice(2), null)
        )

        val snapshot = source.toWidgetSnapshot(fetchedAtMillis = 0L)

        assertEquals(550, snapshot.items[0].tmdbId)
        assertNull(snapshot.items[1].tmdbId)
    }

    @Test
    fun `toWidgetSnapshot keeps all items when fewer than top count`() {
        val source = listOf(BoxOfficeMovie(boxOffice(1), null), BoxOfficeMovie(boxOffice(2), null))

        val snapshot = source.toWidgetSnapshot(fetchedAtMillis = 0L)

        assertEquals(2, snapshot.items.size)
    }

    @Test
    fun `toWidgetSnapshot returns empty snapshot for empty input`() {
        val snapshot = emptyList<BoxOfficeMovie>().toWidgetSnapshot(fetchedAtMillis = 99L)

        assertTrue(snapshot.items.isEmpty())
        assertEquals(99L, snapshot.fetchedAtMillis)
    }

    @Test
    fun `snapshot survives json round trip`() {
        val original = listOf(
            BoxOfficeMovie(boxOffice(1), movie(1, "영화 1")),
            BoxOfficeMovie(boxOffice(2), null)
        ).toWidgetSnapshot(fetchedAtMillis = 7L)

        val decoded = boxOfficeWidgetJson.decodeFromString<BoxOfficeWidgetSnapshot>(
            boxOfficeWidgetJson.encodeToString(original)
        )

        assertEquals(original, decoded)
    }

    @Test
    fun `decoding legacy json with unknown fields does not throw`() {
        val legacyJson = """
            {
              "items": [
                {"rank": 1, "movieName": "영화 1", "audienceCount": 100, "posterPath": "/legacy.jpg"}
              ],
              "fetchedAtMillis": 5,
              "removedField": "ignored"
            }
        """.trimIndent()

        val decoded = boxOfficeWidgetJson.decodeFromString<BoxOfficeWidgetSnapshot>(legacyJson)

        assertEquals(1, decoded.items.size)
        assertEquals("영화 1", decoded.items[0].movieName)
        assertNull(decoded.items[0].tmdbId)
        assertEquals(5L, decoded.fetchedAtMillis)
    }

    @Test(expected = Exception::class)
    fun `strict json rejects unknown fields so ignoreUnknownKeys config is meaningful`() {
        // ignoreUnknownKeys가 실제로 동작하는 설정인지 대조군으로 검증한다.
        Json.decodeFromString<BoxOfficeWidgetSnapshot>(
            """{"items": [], "fetchedAtMillis": 1, "unknown": true}"""
        )
    }
}
