package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.domain.usecase.GetMovieCertificationUseCase
import com.choo.moviefinder.domain.usecase.GetMovieCreditsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieDetailUseCase
import com.choo.moviefinder.domain.usecase.GetMovieRecommendationsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieReviewsUseCase
import com.choo.moviefinder.domain.usecase.GetMovieTrailerUseCase
import com.choo.moviefinder.domain.usecase.GetSimilarMoviesUseCase
import com.choo.moviefinder.domain.usecase.GetWatchProvidersUseCase
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DetailFetchUseCases @Inject constructor(
    val getMovieDetail: GetMovieDetailUseCase,
    val getMovieCredits: GetMovieCreditsUseCase,
    val getSimilarMovies: GetSimilarMoviesUseCase,
    val getMovieTrailer: GetMovieTrailerUseCase,
    val getMovieCertification: GetMovieCertificationUseCase,
    val getMovieReviews: GetMovieReviewsUseCase,
    val getMovieRecommendations: GetMovieRecommendationsUseCase,
    val getWatchProviders: GetWatchProvidersUseCase
)
