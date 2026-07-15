package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.domain.usecase.GetTrailerWatchStatusUseCase
import com.choo.moviefinder.domain.usecase.IsFavoriteUseCase
import com.choo.moviefinder.domain.usecase.IsInWatchlistUseCase
import com.choo.moviefinder.domain.usecase.MarkTrailerWatchedUseCase
import com.choo.moviefinder.domain.usecase.ToggleFavoriteUseCase
import com.choo.moviefinder.domain.usecase.ToggleReviewHelpfulUseCase
import com.choo.moviefinder.domain.usecase.ToggleWatchlistUseCase
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DetailToggleUseCases @Inject constructor(
    val toggleFavorite: ToggleFavoriteUseCase,
    val isFavorite: IsFavoriteUseCase,
    val toggleWatchlist: ToggleWatchlistUseCase,
    val isInWatchlist: IsInWatchlistUseCase,
    val getTrailerWatchStatus: GetTrailerWatchStatusUseCase,
    val markTrailerWatched: MarkTrailerWatchedUseCase,
    val toggleReviewHelpful: ToggleReviewHelpfulUseCase
)
