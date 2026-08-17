package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.remote.api.KmrbApiService
import com.choo.moviefinder.data.remote.dto.KmrbSearchResult
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.remote.parser.KmrbRatingXmlParser
import com.choo.moviefinder.data.util.safeApiCall
import com.choo.moviefinder.domain.model.DomainException
import com.choo.moviefinder.domain.model.KoreanRating
import com.choo.moviefinder.domain.repository.KoreanRatingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class KoreanRatingRepositoryImpl @Inject constructor(
    private val apiService: KmrbApiService,
    private val parser: KmrbRatingXmlParser
) : KoreanRatingRepository {

    override suspend fun searchRatings(title: String, pageNo: Int, numOfRows: Int): List<KoreanRating> {
        if (title.isBlank()) return emptyList()

        val result = safeApiCall {
            val body = apiService.searchMovieRating(title, pageNo, numOfRows)
            // SAX 파싱은 CPU-bound 블로킹 작업이라 메인 스레드(viewModelScope)에서 그대로 돌면
            // 응답 크기와 무관하게 프레임 드랍을 일으킨다 — Default로 옮겨서 실행한다.
            withContext(Dispatchers.Default) {
                body.use { parser.parse(it.byteStream()) }
            }
        }

        if (result.items.isNotEmpty()) {
            return result.items.map { it.toDomain() }
        }

        return when {
            result.isSuccessOrUnknownEmpty() -> emptyList()
            result.isAuthFailure() -> throw DomainException.Unauthorized(
                IllegalStateException(result.resultMsg ?: "Unknown KMRB auth fault")
            )
            else -> {
                Timber.w("영등위 등급 조회 알 수 없는 에러코드: %s (%s)", result.resultCode, result.resultMsg)
                emptyList()
            }
        }
    }

    // resultCode가 없거나 정상 코드("00"/"0")면 단순히 조회 결과가 없는 정상 상태로 간주한다.
    private fun KmrbSearchResult.isSuccessOrUnknownEmpty(): Boolean =
        resultCode == null || resultCode == "00" || resultCode == "0"

    // "30"으로 시작하는 코드 또는 메시지에 SERVICE_KEY가 포함되면 인증키 관련 오류로 간주한다.
    private fun KmrbSearchResult.isAuthFailure(): Boolean =
        resultCode?.startsWith("30") == true || resultMsg?.contains("SERVICE_KEY") == true
}
