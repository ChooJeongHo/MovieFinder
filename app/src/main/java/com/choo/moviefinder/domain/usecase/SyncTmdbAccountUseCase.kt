package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.FavoriteRepository
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.TmdbAuthRepository
import com.choo.moviefinder.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncTmdbAccountUseCase @Inject constructor(
    private val tmdbAuthRepository: TmdbAuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val favoriteRepository: FavoriteRepository,
    private val watchlistRepository: WatchlistRepository
) {
    data class SyncResult(val favoritesAdded: Int, val watchlistAdded: Int)

    suspend operator fun invoke(): SyncResult {
        val (accessToken, accountId) = preferencesRepository.getTmdbAuthOnce()
        requireNotNull(accessToken) { "TMDB 계정이 연결되지 않았습니다" }
        requireNotNull(accountId) { "TMDB 계정 ID가 없습니다" }
        val bearer = "Bearer $accessToken"

        // TMDB 즐겨찾기 목록 가져오기 (1페이지)
        val tmdbFavorites = tmdbAuthRepository.getAccountFavorites(accountId, bearer)
        var favCount = 0
        for (movie in tmdbFavorites) {
            val alreadyFavorite = favoriteRepository.isFavorite(movie.id).first()
            // NOTE: TOCTOU race — FavoriteRepository only exposes toggleFavorite() (no addFavorite()).
            // Another coroutine could add the movie between the isFavorite check and the toggle,
            // causing the toggle to delete it. The window is extremely narrow in practice
            // (sync runs once at login, no concurrent writer expected). A dedicated addFavorite()
            // method would eliminate this entirely but requires a new repository API.
            if (!alreadyFavorite) {
                favoriteRepository.toggleFavorite(movie)
                favCount++
            }
        }

        // TMDB 워치리스트 목록 가져오기 (1페이지)
        val tmdbWatchlist = tmdbAuthRepository.getAccountWatchlist(accountId, bearer)
        var watchCount = 0
        for (movie in tmdbWatchlist) {
            val alreadyInWatchlist = watchlistRepository.isInWatchlist(movie.id).first()
            // NOTE: Same TOCTOU race as above — WatchlistRepository only exposes toggleWatchlist().
            // Race window is narrow; acceptable for a one-shot sync operation.
            if (!alreadyInWatchlist) {
                watchlistRepository.toggleWatchlist(movie)
                watchCount++
            }
        }

        return SyncResult(favCount, watchCount)
    }
}
