package com.choo.moviefinder.data.repository

import com.choo.moviefinder.core.util.titleMatches
import com.choo.moviefinder.data.local.dao.KoreanRatingCacheDao
import com.choo.moviefinder.data.local.entity.KoreanRatingCacheEntity
import com.choo.moviefinder.data.local.entity.toCacheEntity
import com.choo.moviefinder.data.local.entity.toDomain
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
import javax.inject.Inject

class KoreanRatingRepositoryImpl @Inject constructor(
    private val apiService: KmrbApiService,
    private val parser: KmrbRatingXmlParser,
    private val cacheDao: KoreanRatingCacheDao
) : KoreanRatingRepository {

    override suspend fun getRatingForMovie(movieTitle: String): KoreanRating? {
        val trimmed = movieTitle.trim()
        if (trimmed.isBlank()) return null

        val cached = cacheDao.find(trimmed)
        // found=true(등급 확정)는 영구 신뢰, found=false(등급 없음)는 KMRB에 아직 미등록일 뿐일 수
        // 있어(개봉 전 영화 등) 일정 시간 후 재조회하도록 짧은 TTL만 적용한다.
        if (cached != null && (cached.found || !cached.isNegativeCacheExpired())) return cached.toDomain()

        // searchRatings()가 던지는 예외(DomainException.*)는 여기서 잡지 않고 그대로 전파한다 —
        // 일시적 서버 오류를 "등급 없음"으로 캐싱하면 영구 오염되므로 upsert 이전에 예외가
        // 호출부로 빠져나가야 한다.
        val match = searchRatings(trimmed).firstOrNull { candidate -> titleMatches(candidate.title, trimmed) }
        cacheDao.upsert(match.toCacheEntity(trimmed, System.currentTimeMillis()))
        return match
    }

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
            // 알 수 없는 에러코드는 "등급 없음"과 구분해서 예외로 던진다 — 여기서 emptyList()를
            // 반환하면 getRatingForMovie()가 일시적 서버 오류를 영구 negative 캐싱해버린다.
            else -> throw DomainException.Unknown(
                IllegalStateException("영등위 등급 조회 알 수 없는 에러코드: ${result.resultCode} (${result.resultMsg})")
            )
        }
    }

    // resultCode가 없거나 정상 코드("00"/"0")면 단순히 조회 결과가 없는 정상 상태로 간주한다.
    private fun KmrbSearchResult.isSuccessOrUnknownEmpty(): Boolean =
        resultCode == null || resultCode == "00" || resultCode == "0"

    // "30"으로 시작하는 코드 또는 메시지에 SERVICE_KEY가 포함되면 인증키 관련 오류로 간주한다.
    private fun KmrbSearchResult.isAuthFailure(): Boolean =
        resultCode?.startsWith("30") == true || resultMsg?.contains("SERVICE_KEY") == true

    private fun KoreanRatingCacheEntity.isNegativeCacheExpired(): Boolean =
        System.currentTimeMillis() - cachedAt >= NEGATIVE_CACHE_TTL_MS

    companion object {
        // "등급 없음" 캐시는 KMRB에 아직 미등록(개봉 전 등)일 가능성이 있어 영구 신뢰하지 않는다.
        private const val NEGATIVE_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }
}
