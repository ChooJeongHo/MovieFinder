package com.choo.moviefinder.core.util

// URL 쿼리스트링으로 나가는 비밀값의 파라미터 이름.
// 키를 주입하는 인터셉터/Retrofit @Query와 디버그 로그 마스킹 목록(addDebugLogging)이 같은 상수를 쓰므로,
// 이름을 바꿔도 마스킹이 조용히 풀리지 않는다.
object SecretQueryParams {
    const val TMDB_API_KEY = "api_key"
    const val KOFIC_KEY = "key"
    const val KMRB_SERVICE_KEY = "serviceKey"
    const val TMDB_SESSION_ID = "session_id"

    val ALL: List<String> = listOf(TMDB_API_KEY, KOFIC_KEY, KMRB_SERVICE_KEY, TMDB_SESSION_ID)
}
