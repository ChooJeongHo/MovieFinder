package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class SaveWatchHistoryUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(movie: Movie) = repository.saveWatchHistory(movie)
}
