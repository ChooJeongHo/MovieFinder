package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class SaveWatchHistoryUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 영화를 장르 정보와 함께 시청 기록에 저장한다
    suspend operator fun invoke(movie: Movie, genres: String = "") {
        if (genres.isEmpty()) {
            repository.saveWatchHistory(movie)
        } else {
            repository.saveWatchHistoryWithGenres(movie, genres)
        }
    }
}
