package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class SaveSearchQueryUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(query: String) = repository.saveSearchQuery(query)
}