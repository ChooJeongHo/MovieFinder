package com.choo.moviefinder.core.util

import android.content.Context
import android.widget.Toast
import com.choo.moviefinder.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DebugHealthCheck(private val context: Context) {

    // 백그라운드에서 API/이미지/DB 연결을 확인하고 결과를 로그 및 Toast로 표시한다
    fun run(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val results = mutableListOf<String>()

            val apiOk = checkUrl(
                "https://api.themoviedb.org/3/configuration",
                "API"
            )
            results.add(if (apiOk) "API OK" else "API FAIL")

            val imageOk = checkUrl(
                "https://image.tmdb.org/t/p/w92/wwemzKWzjKYJFfCeiB57q3r4Bcm.png",
                "Image"
            )
            results.add(if (imageOk) "Image OK" else "Image FAIL")

            val dbOk = checkDatabase()
            results.add(if (dbOk) "DB OK" else "DB FAIL")

            val summary = "Health: ${results.joinToString(" | ")}"
            Timber.i("헬스체크: $summary")

            if (!apiOk || !imageOk || !dbOk) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkDatabase(): Boolean {
        return try {
            val dbFile = context.getDatabasePath("movie_finder_db")
            dbFile.exists() && dbFile.canRead()
        } catch (e: Exception) {
            Timber.w(e, "DB 헬스체크 실패")
            false
        }
    }

    private fun checkUrl(url: String, tag: String): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .url(
                            chain.request().url.newBuilder()
                                .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                                .build()
                        )
                        .build()
                    chain.proceed(request)
                }
                .build()
            val request = Request.Builder().url(url).head().build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Timber.w(e, "헬스체크 실패: %s", tag)
            false
        }
    }
}
