package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.domain.usecase.IsFavoriteUseCase
import com.choo.moviefinder.domain.usecase.IsInWatchlistUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DetailToggleUseCases @Inject constructor(
    val toggleFavorite: ToggleFavoriteUseCase,
    val isFavorite: IsFavoriteUseCase,
    val toggleWatchlist: ToggleWatchlistUseCase,
    val isInWatchlist: IsInWatchlistUseCase
)
