package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class GetWatchlistUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    operator fun invoke() = repository.getWatchlistMovies()
}
