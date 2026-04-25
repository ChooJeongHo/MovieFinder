package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.WatchProvider
import com.choo.moviefinder.domain.repository.MovieRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetWatchProvidersUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화 ID로 스트리밍 제공 정보를 조회한다
    suspend operator fun invoke(movieId: Int): List<WatchProvider> =
        repository.getWatchProviders(movieId)
}
