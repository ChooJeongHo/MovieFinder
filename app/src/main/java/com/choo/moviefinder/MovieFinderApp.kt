package com.choo.moviefinder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.choo.moviefinder.BuildConfig
import com.choo.moviefinder.core.notification.WatchlistReminderWorker
import com.choo.moviefinder.core.util.AnrWatchdog
import com.choo.moviefinder.core.util.DebugHealthCheck
import com.choo.moviefinder.core.util.FileLoggingTree
import com.choo.moviefinder.di.ImageOkHttpClient
import com.choo.moviefinder.domain.model.ThemeMode
import com.choo.moviefinder.domain.repository.PreferencesRepository
import okhttp3.OkHttpClient
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@HiltAndroidApp
class MovieFinderApp : Application(), SingletonImageLoader.Factory {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun preferencesRepository(): PreferencesRepository

        @ImageOkHttpClient
        fun imageOkHttpClient(): OkHttpClient
    }

    // 앱 초기화 시 알림 채널 생성 및 테마를 적용한다. 디버그 빌드에서는 자가 점검 도구도 시작한다
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        applyTheme()
        if (BuildConfig.DEBUG) {
            Timber.plant(FileLoggingTree(this))
            DebugHealthCheck(this).run(ProcessLifecycleOwner.get().lifecycleScope)
            val watchdog = AnrWatchdog()
            ProcessLifecycleOwner.get().lifecycle.addObserver(watchdog)
            watchdog.start()
        }
    }

    // 개봉일 알림용 및 시청 목표 알림용 NotificationChannel을 생성한다 (API 26+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RELEASE_DATE_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val goalChannel = NotificationChannel(
                WATCH_GOAL_CHANNEL_ID,
                getString(R.string.notification_goal_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_goal_channel_description)
            }
            val reminderChannel = NotificationChannel(
                WatchlistReminderWorker.WATCHLIST_REMINDER_CHANNEL_ID,
                getString(R.string.watchlist_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(goalChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    companion object {
        const val RELEASE_DATE_CHANNEL_ID = "release_date_channel"
        const val WATCH_GOAL_CHANNEL_ID = "watch_goal_channel"
    }

    // 저장된 테마를 동기 적용하고 이후 변경을 실시간 반영한다
    private fun applyTheme() {
        val entryPoint = EntryPointAccessors.fromApplication(this, AppEntryPoint::class.java)
        val repository = entryPoint.preferencesRepository()

        val themeMode = runBlocking { repository.getThemeMode().first() }
        applyNightMode(themeMode)

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            repository.getThemeMode().drop(1).collect { mode ->
                applyNightMode(mode)
            }
        }
    }

    // ThemeMode에 따라 AppCompat 야간 모드를 설정한다
    private fun applyNightMode(themeMode: ThemeMode) {
        val nightMode = when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    // Coil 이미지 로더를 메모리/디스크 캐시와 인증서 피닝을 포함하여 생성한다
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val entryPoint = EntryPointAccessors.fromApplication(this, AppEntryPoint::class.java)
        val imageClient = entryPoint.imageOkHttpClient()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { imageClient }))
            }
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
