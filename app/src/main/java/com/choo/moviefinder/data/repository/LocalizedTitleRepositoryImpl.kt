package com.choo.moviefinder.data.repository

import com.choo.moviefinder.core.util.AppLanguageProvider
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.util.Constants
import com.choo.moviefinder.data.util.safeApiCall
import com.choo.moviefinder.domain.repository.LocalizedTitleRepository
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

// ko-KR로 받은 검색 결과의 표시 제목을 앱 언어로 조회한다. 목록 자체(필터/KMRB 매칭용)는 그대로 두고,
// 화면에 보이는 카드에서만 호출되므로 호출 수는 스크롤 노출량으로 자연스럽게 제한된다.
// Room 영구 캐시 대신 메모리 LRU를 쓰는 이유: TMDB 응답이 cache-control max-age(약 2.8시간)를 내려주고
// OkHttp 디스크 캐시(10MB)가 이를 따르므로, 앱 재시작 후 재조회도 네트워크 없이 처리되어 DB 마이그레이션이 불필요하다.
class LocalizedTitleRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService,
    private val appLanguageProvider: AppLanguageProvider
) : LocalizedTitleRepository {

    // 접근 순서(accessOrder=true) LRU — get()도 내부 순서를 바꾸는 구조 변경이라 모든 접근을 이 객체의 락으로 감싼다.
    private val cache = object : LinkedHashMap<CacheKey, String>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, String>): Boolean =
            size > MAX_CACHE_SIZE
    }

    // 카드가 한꺼번에 컴포즈되어도 동시에 나가는 요청 수를 제한한다 (요청 폭주/레이트 리밋 방지)
    private val lookupPermits = Semaphore(MAX_CONCURRENT_LOOKUPS)

    override fun needsLocalization(): Boolean = appLanguageProvider.currentApiLanguage() != Constants.LANGUAGE_KO

    override fun peekTitle(movieId: Int): String? =
        cachedTitle(CacheKey(movieId, appLanguageProvider.currentApiLanguage()))

    override suspend fun getTitle(movieId: Int): String {
        require(movieId > 0) { "Movie ID must be positive" }
        // 언어는 호출 시점에 한 번만 읽는다 — 조회 중 언어가 바뀌어도 캐시 키와 요청 언어가 어긋나지 않게 함
        val language = appLanguageProvider.currentApiLanguage()
        val key = CacheKey(movieId, language)
        cachedTitle(key)?.let { return it }

        val title = lookupPermits.withPermit {
            safeApiCall { apiService.getMovieDetail(movieId, language).title }
        }
        // 빈 제목을 캐싱하면 이후 재조회 없이 영구히 fallback만 쓰게 되므로 저장하지 않는다
        if (title.isNotBlank()) synchronized(cache) { cache[key] = title }
        return title
    }

    private fun cachedTitle(key: CacheKey): String? = synchronized(cache) { cache[key] }

    // 언어를 키에 포함해 앱 언어를 바꿔도 이전 언어 제목이 섞이지 않게 한다
    private data class CacheKey(val movieId: Int, val language: String)

    private companion object {
        const val MAX_CACHE_SIZE = 500
        const val MAX_CONCURRENT_LOOKUPS = 4
        const val INITIAL_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
    }
}
