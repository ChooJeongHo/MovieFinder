package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieQueryRepository
import dagger.Reusable
import javax.inject.Inject

// 앱 언어 변경 시 현재 상영작/인기 영화 로컬 캐시를 무효화한다. RemoteMediator의 1시간 TTL과
// 무관하게 즉시 비워, 이전 언어의 제목/줄거리가 화면에 남아있는 것을 방지한다.
@Reusable
class InvalidateHomeMovieCacheUseCase @Inject constructor(
    private val movieQueryRepository: MovieQueryRepository
) {
    suspend operator fun invoke() = movieQueryRepository.invalidateHomeMovieCache()
}
