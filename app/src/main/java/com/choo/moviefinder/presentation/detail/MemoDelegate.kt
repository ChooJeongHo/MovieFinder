package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.domain.model.Memo
import com.choo.moviefinder.domain.usecase.DeleteMemoUseCase
import com.choo.moviefinder.domain.usecase.GetMemosUseCase
import com.choo.moviefinder.domain.usecase.SaveMemoUseCase
import com.choo.moviefinder.domain.usecase.UpdateMemoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// 영화 메모 관련 상태와 액션을 담당하는 위임 클래스
class MemoDelegate(
    getMemosUseCase: GetMemosUseCase,
    private val saveMemoUseCase: SaveMemoUseCase,
    private val updateMemoUseCase: UpdateMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase,
    private val movieId: Int,
    private val viewModelScope: CoroutineScope,
    private val snackbarChannel: Channel<ErrorType>
) {

    val memos: StateFlow<List<Memo>> = getMemosUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 영화에 새 메모를 저장
    fun saveMemo(content: String) = viewModelScope.launchWithErrorHandler(
        onError = { snackbarChannel.trySend(it) }
    ) {
        saveMemoUseCase(movieId, content)
    }

    // 기존 메모 내용을 수정
    fun updateMemo(memoId: Long, content: String) = viewModelScope.launchWithErrorHandler(
        onError = { snackbarChannel.trySend(it) }
    ) {
        updateMemoUseCase(memoId, content)
    }

    // 메모를 삭제
    fun deleteMemo(memoId: Long) = viewModelScope.launchWithErrorHandler(
        onError = { snackbarChannel.trySend(it) }
    ) {
        deleteMemoUseCase(memoId)
    }
}
