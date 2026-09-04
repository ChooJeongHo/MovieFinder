package com.choo.moviefinder.core.shortcut

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.choo.moviefinder.R
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.usecase.GetRecentShortcutMoviesUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShortcutManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getRecentShortcutMovies: GetRecentShortcutMoviesUseCase
) {

    private val refreshMutex = Mutex()

    // 최근 시청 영화로 동적 단축키 목록을 전량 교체한다 (항상 정확히 MAX_SHORTCUT_MOVIES개 이하 유지).
    // 읽기(getRecentShortcutMovies)와 쓰기(setDynamicShortcuts) 사이에 suspension point가 있어
    // 동시 호출 시 인터리빙되면 최신 갱신이 오래된 스냅샷으로 덮일 수 있으므로 하나의 임계 구역으로 묶는다.
    suspend fun refreshRecentMovieShortcuts() {
        refreshMutex.withLock {
            val shortcuts = getRecentShortcutMovies().map { buildMovieShortcut(it) }
            // setDynamicShortcuts는 시스템 ShortcutService로의 동기 바인더 IPC라 호출 스레드를 블로킹한다.
            withContext(Dispatchers.IO) {
                ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
            }
        }
    }

    // 영화 하나를 동적 단축키(ShortcutInfoCompat)로 변환한다
    internal fun buildMovieShortcut(movie: Movie): ShortcutInfoCompat {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("moviefinder://movie/${movie.id}")).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return ShortcutInfoCompat.Builder(context, "$SHORTCUT_ID_PREFIX${movie.id}")
            .setShortLabel(movie.title)
            .setLongLabel(movie.title)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_movie))
            .setIntent(intent)
            .build()
    }

    companion object {
        private const val SHORTCUT_ID_PREFIX = "recent_movie_"
    }
}
