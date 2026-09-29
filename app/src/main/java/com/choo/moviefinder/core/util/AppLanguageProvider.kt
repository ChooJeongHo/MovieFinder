package com.choo.moviefinder.core.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

// 현재 앱 언어(Per-App Language)를 TMDB API의 language 쿼리 파라미터 형식으로 변환해 제공한다.
// AppCompatDelegate에 앱 언어가 설정되어 있지 않으면(시스템 언어 따름) 실제 유효 로케일인
// Resources.configuration.locales로 폴백한다.
class AppLanguageProvider(private val context: Context) {

    fun currentApiLanguage(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val appLocaleLanguage = if (appLocales.isEmpty) null else appLocales[0]?.language
        val systemLocaleLanguage = context.resources.configuration.locales[0]?.language
        return resolveApiLanguage(appLocaleLanguage, systemLocaleLanguage)
    }
}

// locales_config.xml이 ko/en만 지원하므로, en이 아니면 전부 ko로 취급한다.
// dynamicDark/dynamicLight 콜백 분리(MovieFinderTheme)와 같은 이유로, Context/AppCompatDelegate
// 정적 호출과 분기 로직을 분리해 JVM 유닛 테스트로 검증할 수 있게 했다.
internal fun resolveApiLanguage(appLocaleLanguage: String?, systemLocaleLanguage: String?): String =
    if ((appLocaleLanguage ?: systemLocaleLanguage) == "en") API_LANGUAGE_EN else API_LANGUAGE_KO

private const val API_LANGUAGE_KO = "ko-KR"
private const val API_LANGUAGE_EN = "en-US"
