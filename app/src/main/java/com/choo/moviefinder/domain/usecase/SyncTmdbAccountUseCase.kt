package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.data.remote.api.TmdbAuthApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.domain.repository.FavoriteRepository
import com.choo.moviefinder.domain.repository.PreferencesRepository
import com.choo.moviefinder.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncTmdbAccountUseCase @Inject constructor(
    private val authApiService: TmdbAuthApiService,
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
        val tmdbFavorites = authApiService.getAccountFavorites(accountId, bearer).results
        var favCount = 0
        for (dto in tmdbFavorites) {
            val alreadyFavorite = favoriteRepository.isFavorite(dto.id).first()
            if (!alreadyFavorite) {
                favoriteRepository.toggleFavorite(dto.toDomain())
                favCount++
            }
        }

        // TMDB 워치리스트 목록 가져오기 (1페이지)
        val tmdbWatchlist = authApiService.getAccountWatchlist(accountId, bearer).results
        var watchCount = 0
        for (dto in tmdbWatchlist) {
            val alreadyInWatchlist = watchlistRepository.isInWatchlist(dto.id).first()
            if (!alreadyInWatchlist) {
                watchlistRepository.toggleWatchlist(dto.toDomain())
                watchCount++
            }
        }

        return SyncResult(favCount, watchCount)
    }
}
