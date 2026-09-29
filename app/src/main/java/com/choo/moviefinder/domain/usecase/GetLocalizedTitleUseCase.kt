package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.core.util.suspendRunCatching
import com.choo.moviefinder.domain.repository.LocalizedTitleRepository
import dagger.Reusable
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

// 검색 결과 카드의 표시 제목을 앱 언어로 바꿔 준다. 목록 데이터(ko-KR)는 그대로 두고 화면에 컴포즈된 카드에서만
// 호출되며, 실패하거나 앱 언어가 한국어면 항상 fallbackTitle(ko-KR)을 돌려주므로 UI는 예외를 신경 쓰지 않는다.
@Reusable
class GetLocalizedTitleUseCase @Inject constructor(
    private val repository: LocalizedTitleRepository
) {

    // 네트워크 없이 즉시 쓸 수 있는 제목 — 이미 조회한 카드가 다시 컴포즈될 때 한국어가 한 프레임 비치는 깜빡임을 막는다
    fun peek(movieId: Int, fallbackTitle: String): String {
        if (!repository.needsLocalization()) return fallbackTitle
        return repository.peekTitle(movieId) ?: fallbackTitle
    }

    suspend operator fun invoke(movieId: Int, fallbackTitle: String): String {
        if (!repository.needsLocalization()) return fallbackTitle
        repository.peekTitle(movieId)?.let { return it }

        // 빠른 스크롤로 스쳐 지나가는 카드는 이 대기 중에 컴포지션에서 벗어나 코루틴이 취소되므로 요청이 나가지 않는다
        delay(LOOKUP_DEBOUNCE_MS)

        return suspendRunCatching { repository.getTitle(movieId) }
            .onFailure { Timber.w(it, "표시 제목 조회 실패: movieId=%d", movieId) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: fallbackTitle
    }

    private companion object {
        const val LOOKUP_DEBOUNCE_MS = 150L
    }
}
