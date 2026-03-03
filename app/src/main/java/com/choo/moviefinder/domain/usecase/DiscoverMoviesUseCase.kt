package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class DiscoverMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    operator fun invoke(
        genres: Set<Int> = emptySet(),
        sortBy: String = "popularity.desc",
        year: Int? = null
    ) = repository.discoverMovies(genres, sortBy, year)
}
