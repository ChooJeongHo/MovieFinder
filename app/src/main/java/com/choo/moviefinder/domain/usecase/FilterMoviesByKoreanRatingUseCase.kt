package com.choo.moviefinder.domain.usecase

import androidx.paging.PagingData
import androidx.paging.filter
import com.choo.moviefinder.domain.model.KoreanRatingGrade
import com.choo.moviefinder.domain.model.Movie
import dagger.Reusable
import javax.inject.Inject

// KMRB에 배치 조회 엔드포인트가 없어 항목별 단건 조회 외에 총 호출 수를 줄일 수단이 없다.
// 항목은 순차로 조회된다(콜드 0.4~0.55초/건). PagingData.filter는 페이지가 도착하면 그 페이지(TMDB 1페이지=20건,
// BaseMoviePagingSource가 loadSize를 무시하므로 initialLoadSize=60이어도 첫 로드는 20건)를 통째로 평가한 뒤
// 전달하므로 첫 결과까지 약 10초가 걸리고, 통과 항목이 적으면 Paging이 페이지를 연쇄 로드한다
// (실측 2026-09-29: knight + 15+ → 15페이지·307건·138초). 두 번째부터는 Room 캐시(KoreanRatingRepositoryImpl)로
// 즉시 응답한다. 병렬화/청크 단위 필터 등 추가 최적화는 이번 범위 밖이다.
//
// 호출 위치가 중요하다: SearchViewModel은 이 필터를 cachedIn *앞*에서 적용하고 등급이 바뀌면 재검색한다. cachedIn 뒤에서
// 재적용하면, 이미 2페이지 이상 로드된 상태에서 전부 걸러질 때 Paging이 이어 로드를 멈춘다(페이지 기아 —
// PagingEmptyStateRegressionTest 참고). 1페이지만 로드된 상태에선 Paging이 스스로 이어 로드해 재현되지 않는다.
//
// 검색 결과(movie.title)는 앱 언어와 무관하게 항상 ko-KR로 고정된다(MoviePagingSource 참고) — 이 필터가
// movie.title로 KMRB에 직접 매칭하기 때문이다. 앱 언어를 따르는 것은 화면 표시 제목뿐이며 카드 단위로만 지연
// 조회한다(GetLocalizedTitleUseCase). 필터 안에서 항목마다 한국어 제목을 추가 조회하던 방식은 항목당 호출이
// 2배(제목 + KMRB)가 되어 약 68초(2026-09-28 SM-S926N)가 걸려 폐기했다.
@Reusable
class FilterMoviesByKoreanRatingUseCase @Inject constructor(
    private val getKoreanRatingUseCase: GetKoreanRatingUseCase
) {
    suspend operator fun invoke(pagingData: PagingData<Movie>, grade: KoreanRatingGrade?): PagingData<Movie> {
        if (grade == null) return pagingData
        return pagingData.filter { movie -> getKoreanRatingUseCase(movie.title)?.gradeName == grade.apiGradeName }
    }
}
