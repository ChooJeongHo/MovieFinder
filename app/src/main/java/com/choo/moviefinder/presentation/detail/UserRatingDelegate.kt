package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.domain.usecase.DeleteUserRatingUseCase
import com.choo.moviefinder.domain.usecase.GetUserRatingUseCase
import com.choo.moviefinder.domain.usecase.SetUserRatingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

// 사용자 영화 평점 관련 상태와 액션을 담당하는 위임 클래스
class UserRatingDelegate(
    getUserRatingUseCase: GetUserRatingUseCase,
    private val setUserRatingUseCase: SetUserRatingUseCase,
    private val deleteUserRatingUseCase: DeleteUserRatingUseCase,
    private val movieId: Int,
    private val viewModelScope: CoroutineScope,
    private val snackbarChannel: Channel<ErrorType>
) {

    val userRating: StateFlow<Float?> = getUserRatingUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 사용자 영화 평점을 Room DB에 저장
    fun setUserRating(rating: Float) = viewModelScope.launchWithErrorHandler(
        onError = { snackbarChannel.send(it) }
    ) {
        setUserRatingUseCase(movieId, rating)
        Timber.d("User rating set to %.1f for movie %d", rating, movieId)
    }

    // 사용자 영화 평점을 Room DB에서 삭제
    fun deleteUserRating() = viewModelScope.launchWithErrorHandler(
        onError = { snackbarChannel.send(it) }
    ) {
        deleteUserRatingUseCase(movieId)
        Timber.d("User rating deleted for movie %d", movieId)
    }
}
