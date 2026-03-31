package com.choo.moviefinder.core.util

import android.content.Context
import android.widget.Toast
import com.choo.moviefinder.BuildConfig
import com.choo.moviefinder.data.local.MovieDatabase
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
                "https://api.themoviedb.org/3/configuration?api_key=${BuildConfig.TMDB_API_KEY}"
            )
            results.add(if (apiOk) "API OK" else "API FAIL")

            val imageOk = checkUrl(
                "https://image.tmdb.org/t/p/w92/wwemzKWzjKYJFfCeiB57q3r4Bcm.png"
            )
            results.add(if (imageOk) "Image OK" else "Image FAIL")

            val dbOk = checkDatabase()
            results.add(if (dbOk) "DB OK" else "DB FAIL")

            val summary = "Health: ${results.joinToString(" | ")}"
            Timber.i("HealthCheck: $summary")

            if (!apiOk || !imageOk || !dbOk) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkDatabase(): Boolean {
        return try {
            val db = androidx.room.Room.databaseBuilder(
                context,
                MovieDatabase::class.java,
                "movie_finder_db"
            ).build()
            db.openHelper.readableDatabase
            db.close()
            true
        } catch (e: Exception) {
            Timber.e(e, "HealthCheck: DB check failed")
            false
        }
    }

    private fun checkUrl(url: String): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).head().build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Timber.w(e, "HealthCheck: URL check failed for $url")
            false
        }
    }
}
