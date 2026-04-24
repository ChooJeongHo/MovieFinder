package com.choo.moviefinder.presentation.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.WhileSubscribed5s
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.domain.model.ScheduledReminder
import com.choo.moviefinder.domain.usecase.CancelReminderUseCase
import com.choo.moviefinder.domain.usecase.GetScheduledRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ReminderHistoryViewModel @Inject constructor(
    getScheduledRemindersUseCase: GetScheduledRemindersUseCase,
    private val cancelReminderUseCase: CancelReminderUseCase
) : ViewModel() {

    // 예약된 알림 목록 (개봉일 오름차순)
    val reminders: StateFlow<List<ScheduledReminder>> = getScheduledRemindersUseCase()
        .stateIn(viewModelScope, WhileSubscribed5s, emptyList())

    private val _cancelledEvent = Channel<Unit>(Channel.CONFLATED)
    val cancelledEvent = _cancelledEvent.receiveAsFlow()

    private val _errorEvent = Channel<Unit>(Channel.CONFLATED)
    val errorEvent = _errorEvent.receiveAsFlow()

    // 특정 영화의 알림을 취소하고 DB에서 삭제한다
    fun cancelReminder(movieId: Int) = viewModelScope.launchWithErrorHandler(
        onError = {
            Timber.e("알림 취소 실패: movieId=%d", movieId)
            _errorEvent.trySend(Unit)
        }
    ) {
        cancelReminderUseCase(movieId)
        _cancelledEvent.trySend(Unit)
    }
}
