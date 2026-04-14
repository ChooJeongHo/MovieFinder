package com.choo.moviefinder.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 코루틴 실행 후 예외 발생 시 ErrorType으로 변환하여 에러 핸들러를 호출한다
fun CoroutineScope.launchWithErrorHandler(
    onError: suspend (ErrorType) -> Unit,
    block: suspend () -> Unit
) {
    launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(ErrorMessageProvider.getErrorType(e))
        }
    }
}

// CancellationException을 재전파하는 runCatching 대체 헬퍼
inline fun <T> suspendRunCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
