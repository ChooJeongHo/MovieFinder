package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.repository.FavoriteRepository
import com.choo.moviefinder.domain.repository.WatchlistRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class SearchLocalMoviesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val watchlistRepository: WatchlistRepository
) {
    // 즐겨찾기 + 워치리스트에서 제목으로 검색하여 중복 제거 후 반환한다
    suspend operator fun invoke(query: String): List<Movie> {
        val favorites = favoriteRepository.searchFavoriteMovies(query)
        val watchlist = watchlistRepository.searchWatchlistMovies(query)
        return (favorites + watchlist).distinctBy { it.id }
    }
}
