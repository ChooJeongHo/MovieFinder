package com.choo.moviefinder.di

import com.choo.moviefinder.BuildConfig
import com.choo.moviefinder.core.util.SecretQueryParams
import com.sun.net.httpserver.HttpServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList

// 디버그 빌드의 OkHttp 로그에 키/토큰이 남지 않는지, 실제 클라이언트 빌더 조립 그대로 검증한다.
// release 변형에서는 addDebugLogging()이 no-op이라 검증할 로그가 없으므로 클래스 전체를 건너뛴다
// (testDebugUnitTest에서만 실행됨 — CI와 pre-push가 이 태스크를 돌린다).
// 비밀값은 전부 가짜 문자열이며 실제 API는 호출하지 않는다(로컬 HttpServer).
class NetworkLogRedactionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: HttpServer
    private val clients = mutableListOf<OkHttpClient>()
    private val logLines = CopyOnWriteArrayList<String>()

    // OkHttp 로그 + DebugEventListener 등 Timber로 나가는 모든 출력(예외 스택 포함)을 수집한다
    private val captureTree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            logLines += if (t != null) "$message\n${t.stackTraceToString()}" else message
        }
    }

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                val body = "{}".toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        Timber.plant(captureTree)
    }

    @After
    fun tearDown() {
        Timber.uproot(captureTree)
        server.stop(0)
        clients.forEach {
            it.dispatcher.executorService.shutdown()
            it.connectionPool.evictAll()
            it.cache?.close()
        }
    }

    @Test
    fun `KMRB serviceKey는 요청 줄과 응답 줄 모두에서 가려진다`() {
        val client = track(NetworkModule.buildKmrbOkHttpClient(KMRB_SECRET))

        call(client, "/movie_v3/movie_search_v3?title=abc")

        assertNoSecret(KMRB_SECRET)
        assertMasked(SecretQueryParams.KMRB_SERVICE_KEY, requestLine = true, responseLine = true)
    }

    @Test
    fun `KOFIC key는 요청 줄과 응답 줄 모두에서 가려진다`() {
        val client = track(NetworkModule.buildKoficOkHttpClient(KOFIC_SECRET))

        call(client, "/boxoffice/searchDailyBoxOfficeList.json?targetDt=20260101")

        assertNoSecret(KOFIC_SECRET)
        assertMasked(SecretQueryParams.KOFIC_KEY, requestLine = true, responseLine = true)
    }

    // TMDB v3는 로깅이 키 주입보다 먼저 등록돼 요청 줄에는 키가 없다.
    // 그래도 응답 줄은 키가 주입된 최종 요청 URL을 찍으므로 마스킹이 필요하다.
    @Test
    fun `TMDB api_key는 로깅이 키 주입보다 앞에 있어도 응답 줄에서 가려진다`() {
        val client = track(NetworkModule.buildTmdbOkHttpClient(tempFolder.newFolder(), TMDB_SECRET))

        call(client, "/3/movie/550?language=ko-KR")

        assertNoSecret(TMDB_SECRET)
        assertMasked(SecretQueryParams.TMDB_API_KEY, requestLine = false, responseLine = true)
    }

    @Test
    fun `TMDB session_id는 호출부가 직접 붙인 쿼리여도 가려진다`() {
        val client = track(NetworkModule.buildTmdbOkHttpClient(tempFolder.newFolder(), TMDB_SECRET))

        call(client, "/3/movie/550/rating?session_id=$SESSION_SECRET")

        assertNoSecret(SESSION_SECRET)
        assertMasked(SecretQueryParams.TMDB_SESSION_ID, requestLine = true, responseLine = true)
    }

    @Test
    fun `TMDB v4 Bearer 토큰은 Authorization 헤더 줄에서 가려진다`() {
        val client = track(NetworkModule.buildTmdbV4OkHttpClient(V4_SECRET))

        call(client, "/4/auth/request_token")

        assertNoSecret(V4_SECRET)
        assertTrue(
            "헤더 마스킹 줄(Authorization: ██)이 로그에 있어야 한다. 로그: $logLines",
            logLines.any { it == "Authorization: $HEADER_MASK" }
        )
    }

    @Test
    fun `연결 실패 줄(HTTP FAILED)에서도 키가 가려진다`() {
        val client = track(NetworkModule.buildKmrbOkHttpClient(KMRB_SECRET))

        callClosedPort(client, "/movie_v3/movie_search_v3")

        assertNoSecret(KMRB_SECRET)
        val maskedParam = "${SecretQueryParams.KMRB_SERVICE_KEY}=$URL_MASK"
        assertTrue(
            "HTTP FAILED 줄에 마스킹된 serviceKey가 있어야 한다. 로그: $logLines",
            logLines.any { it.startsWith("<-- HTTP FAILED") && maskedParam in it }
        )
    }

    // DebugEventListener.callFailed는 인터셉터가 키를 주입하기 전의 원본 URL을 찍는다.
    // 주입되는 키는 거기 없지만, 호출부가 @Query로 직접 붙이는 session_id는 원본 URL에 이미 들어 있다.
    // Timber.e라 INFO 이상 → FileLoggingTree가 공유 가능한 debug_log.txt에도 기록한다.
    @Test
    fun `호출 실패 시 이벤트 리스너 로그에서도 session_id가 가려진다`() {
        val client = track(NetworkModule.buildTmdbOkHttpClient(tempFolder.newFolder(), TMDB_SECRET))

        callClosedPort(client, "/3/movie/550/rating?session_id=$SESSION_SECRET")

        // 리스너 출력이 실제로 수집됐어야 "리스너 로그에 없다"는 단언이 의미를 갖는다
        val listenerLines = logLines.filter { it.contains("호출 실패") }
        assertTrue("DebugEventListener의 호출 실패 로그가 있어야 한다. 로그: $logLines", listenerLines.isNotEmpty())
        assertNoSecret(SESSION_SECRET)
        assertNoSecret(TMDB_SECRET)
        val maskedParam = "${SecretQueryParams.TMDB_SESSION_ID}=$URL_MASK"
        assertTrue(
            "리스너 로그의 URL에 마스킹된 session_id가 있어야 한다. 로그: $listenerLines",
            listenerLines.any { maskedParam in it }
        )
    }

    private fun track(client: OkHttpClient): OkHttpClient = client.also { clients += it }

    private fun call(client: OkHttpClient, pathAndQuery: String) {
        val url = "http://127.0.0.1:${server.address.port}$pathAndQuery"
        client.newCall(Request.Builder().url(url).build()).execute().use { it.body.string() }
    }

    // 닫힌 포트로 호출해 연결 실패(HTTP FAILED 줄, 이벤트 리스너 callFailed)를 일으키고, 실패했는지까지 확인한다
    private fun callClosedPort(client: OkHttpClient, pathAndQuery: String) {
        val closedPort = ServerSocket(0).use { it.localPort }
        val failure = runCatching {
            client.newCall(Request.Builder().url("http://127.0.0.1:$closedPort$pathAndQuery").build())
                .execute().close()
        }
        assertTrue("닫힌 포트 호출은 실패해야 한다", failure.isFailure)
    }

    private fun assertNoSecret(secret: String) {
        val leaked = logLines.filter { secret in it }
        assertTrue("비밀값이 로그에 ${leaked.size}줄 남았다: $leaked", leaked.isEmpty())
    }

    // 마스킹된 줄이 실제로 있는지도 확인한다 — 로깅이 꺼져 있어서 "노출 0줄"이 되는 위양성을 막는다
    private fun assertMasked(name: String, requestLine: Boolean, responseLine: Boolean) {
        val masked = "$name=$URL_MASK"
        if (requestLine) {
            assertTrue(
                "요청 줄에 $masked 가 있어야 한다. 로그: $logLines",
                logLines.any { it.startsWith("--> ") && masked in it }
            )
        }
        if (responseLine) {
            assertTrue(
                "응답 줄에 $masked 가 있어야 한다. 로그: $logLines",
                logLines.any { it.startsWith("<-- ") && masked in it }
            )
        }
    }

    companion object {
        private const val KMRB_SECRET = "FAKE_KMRB_SECRET_0001"
        private const val KOFIC_SECRET = "FAKE_KOFIC_SECRET_0002"
        private const val TMDB_SECRET = "FAKE_TMDB_SECRET_0003"
        private const val SESSION_SECRET = "FAKE_SESSION_SECRET_0004"
        private const val V4_SECRET = "FAKE_V4_SECRET_0005"

        // HttpLoggingInterceptor는 쿼리 값을 "██"로 바꾸고, URL에 들어가면서 퍼센트 인코딩된다
        private const val URL_MASK = "%E2%96%88%E2%96%88"
        private const val HEADER_MASK = "██"

        // release 변형은 로깅 자체가 없으므로 클래스 전체를 건너뛴다 (@Before/@After도 실행되지 않음)
        @JvmStatic
        @BeforeClass
        fun requireDebugVariant() {
            assumeTrue("디버그 변형에서만 의미가 있다", BuildConfig.DEBUG)
        }
    }
}
