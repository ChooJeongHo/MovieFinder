package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.core.util.suspendRunCatching
import com.choo.moviefinder.domain.usecase.GetTrailerWatchStatusUseCase
import com.choo.moviefinder.domain.usecase.MarkTrailerWatchedUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

// 예고편 이어보기 확인 다이얼로그 관련 상태와 액션을 담당하는 위임 클래스
class TrailerWatchDelegate(
    private val getTrailerWatchStatusUseCase: GetTrailerWatchStatusUseCase,
    private val markTrailerWatchedUseCase: MarkTrailerWatchedUseCase,
    private val movieId: Int,
    private val viewModelScope: CoroutineScope
) {

    private val _resumeTrailerEvent = Channel<String>(Channel.CONFLATED)
    val resumeTrailerEvent: Flow<String> = _resumeTrailerEvent.receiveAsFlow()

    // 이전에 이 영화의 트레일러를 시청한 기록이 있으면 이어보기 확인 이벤트를 전송한다
    // 실패해도 조용히 무시 (부가 기능이 메인 플로우를 깨면 안 됨)
    suspend fun checkAndPromptIfWatched(trailerKey: String) {
        suspendRunCatching { getTrailerWatchStatusUseCase(movieId) }
            .onSuccess { watch -> if (watch != null) _resumeTrailerEvent.trySend(trailerKey) }
            .onFailure { Timber.w(it, "영화 %d 트레일러 시청 기록 조회 실패", movieId) }
    }

    // 트레일러 시청 시작을 fire-and-forget으로 기록한다
    fun markTrailerWatched(trailerKey: String): Job = viewModelScope.launch {
        suspendRunCatching { markTrailerWatchedUseCase(movieId, trailerKey) }
            .onFailure { Timber.w(it, "영화 %d 트레일러 시청 기록 저장 실패", movieId) }
    }
}
