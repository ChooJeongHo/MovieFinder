package com.choo.moviefinder.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageProviderTest {

    @Test
    fun `앱 언어가 en이면 시스템 언어와 무관하게 en-US를 반환한다`() {
        assertEquals("en-US", resolveApiLanguage(appLocaleLanguage = "en", systemLocaleLanguage = "ko"))
    }

    @Test
    fun `앱 언어가 ko이면 en-US가 아니라 ko-KR을 반환한다`() {
        assertEquals("ko-KR", resolveApiLanguage(appLocaleLanguage = "ko", systemLocaleLanguage = "en"))
    }

    @Test
    fun `앱 언어 미설정 시 시스템 언어가 en이면 en-US로 폴백한다`() {
        assertEquals("en-US", resolveApiLanguage(appLocaleLanguage = null, systemLocaleLanguage = "en"))
    }

    @Test
    fun `앱 언어와 시스템 언어 모두 없으면 ko-KR로 기본 폴백한다`() {
        assertEquals("ko-KR", resolveApiLanguage(appLocaleLanguage = null, systemLocaleLanguage = null))
    }

    @Test
    fun `locales_config에 없는 언어(예 ja)는 ko-KR로 취급한다`() {
        assertEquals("ko-KR", resolveApiLanguage(appLocaleLanguage = "ja", systemLocaleLanguage = null))
    }
}
