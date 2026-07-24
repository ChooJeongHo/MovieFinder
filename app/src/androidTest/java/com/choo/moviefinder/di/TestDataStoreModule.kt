package com.choo.moviefinder.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.choo.moviefinder.data.local.UserSettings
import com.choo.moviefinder.data.local.UserSettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Singleton

// HiltAndroidRule은 @Test 메서드마다 새 SingletonComponent를 만든다. DataStoreModule의
// provideUserSettingsDataStore()는 매번 같은 파일(datastore/user_settings.json)을 가리키는
// 새 DataStore 인스턴스를 생성하는데, 이전 인스턴스의 내부 코디네이터가 아직 그 파일을 활성 상태로
// 붙잡고 있으면 DataStore가 "There are multiple DataStores active for the same file"로 크래시한다.
// 테스트 실행마다 고유한 임시 파일을 쓰도록 교체해 파일 경합 자체를 없앤다.
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataStoreModule::class]
)
object TestDataStoreModule {

    // Preferences DataStore는 Context 확장 프로퍼티(by preferencesDataStore)에 인스턴스가
    // 캐싱되어 같은 프로세스 내에서는 항상 동일 객체가 재사용되므로 테스트에서도 안전하다.
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    // 매 테스트마다 고유한 임시 파일을 사용해 인스턴스 간 파일 경합을 방지한다.
    // UserSettings.onboardingCompleted 기본값은 false라 앱 시작 목적지가 onboardingFragment인데,
    // Espresso 테스트들은 전부 홈 화면에서 시작한다고 가정하므로 온보딩 완료 상태로 미리 시드한다.
    @Provides
    @Singleton
    fun provideUserSettingsDataStore(@ApplicationContext context: Context): DataStore<UserSettings> {
        val dataStore = DataStoreFactory.create(
            serializer = UserSettingsSerializer,
            produceFile = {
                File.createTempFile("test_user_settings", ".json", context.cacheDir)
            }
        )
        runBlocking { dataStore.updateData { it.copy(onboardingCompleted = true) } }
        return dataStore
    }
}
