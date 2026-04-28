package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.domain.usecase.DeleteUserRatingUseCase
import com.choo.moviefinder.domain.usecase.GetTmdbAccessTokenUseCase
import com.choo.moviefinder.domain.usecase.GetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.SetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.SubmitTmdbRatingUseCase
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DetailRatingUseCases @Inject constructor(
    val getUserRating: GetUserRatingUseCase,
    val setUserRating: SetUserRatingUseCase,
    val deleteUserRating: DeleteUserRatingUseCase,
    val getTmdbAccessToken: GetTmdbAccessTokenUseCase,
    val submitTmdbRating: SubmitTmdbRatingUseCase
)
