package com.choo.moviefinder.presentation.widget

import com.choo.moviefinder.domain.model.BoxOfficeMovie
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 박스오피스 위젯이 Glance state(Preferences)에 보관하는 렌더링용 스냅샷.
// Worker가 네트워크에서 받아온 결과를 이 형태로 직렬화해두고, Glance는 이것만 읽어 그린다.
// 이 파일은 Android 프레임워크에 의존하지 않는 순수 Kotlin이며 JVM 유닛 테스트 대상이다.
@Serializable
internal data class BoxOfficeWidgetSnapshot(
    val items: List<BoxOfficeWidgetItem>,
    val fetchedAtMillis: Long
)

@Serializable
internal data class BoxOfficeWidgetItem(
    val rank: Int,
    val movieName: String,
    val audienceCount: Long,
    // TMDB 매칭에 실패하면 null — 이 경우 항목을 눌러도 상세 화면 딥링크를 열 수 없다.
    val tmdbId: Int? = null
)

// 구버전 스냅샷에 없던 필드가 추가되거나 필드가 제거돼도 역직렬화가 깨지지 않도록 관대하게 설정.
// (PopularMoviesRemoteViewsFactory와 동일 구성)
internal val boxOfficeWidgetJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

internal fun List<BoxOfficeMovie>.toWidgetSnapshot(
    fetchedAtMillis: Long,
    topCount: Int = BoxOfficeWidget.TOP_COUNT
): BoxOfficeWidgetSnapshot = BoxOfficeWidgetSnapshot(
    items = this.sortedBy { it.boxOffice.rank }
        .take(topCount)
        .map { boxOfficeMovie ->
            BoxOfficeWidgetItem(
                rank = boxOfficeMovie.boxOffice.rank,
                movieName = boxOfficeMovie.boxOffice.movieName,
                audienceCount = boxOfficeMovie.boxOffice.audienceCount,
                tmdbId = boxOfficeMovie.matchedMovie?.id
            )
        },
    fetchedAtMillis = fetchedAtMillis
)
