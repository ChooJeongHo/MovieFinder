package com.choo.moviefinder.domain.usecase

import androidx.paging.PagingData
import androidx.paging.filter
import com.choo.moviefinder.domain.model.KoreanRatingGrade
import com.choo.moviefinder.domain.model.Movie
import dagger.Reusable
import javax.inject.Inject

// KMRB에 배치 조회 엔드포인트가 없어 항목별 단건 조회 외에 총 호출 수를 줄일 수단이 없다.
// 필터 활성 상태로 처음 보는 페이지는 DEFAULT_PAGING_CONFIG.initialLoadSize(PAGE_SIZE*3=60)개
// 항목이 순차 조회되어 초기 지연이 있고, 두 번째부터는 Room 캐시(KoreanRatingRepositoryImpl)로
// 즉시 응답한다. 이번 범위에서 병렬화 등 추가 동시성 최적화는 하지 않는다.
@Reusable
class FilterMoviesByKoreanRatingUseCase @Inject constructor(
    private val getKoreanRatingUseCase: GetKoreanRatingUseCase
) {
    suspend operator fun invoke(pagingData: PagingData<Movie>, grade: KoreanRatingGrade?): PagingData<Movie> {
        if (grade == null) return pagingData
        return pagingData.filter { movie -> getKoreanRatingUseCase(movie.title)?.gradeName == grade.apiGradeName }
    }
}
