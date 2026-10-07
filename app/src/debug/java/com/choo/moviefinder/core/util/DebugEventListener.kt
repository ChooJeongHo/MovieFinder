package com.choo.moviefinder.core.util

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.HttpUrl
import okhttp3.Protocol
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

class DebugEventListener : EventListener() {
    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException
    ) {
        Timber.e(ioe, "🔴 연결 실패: ${call.request().url.host}")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        if (handshake != null) {
            Timber.d("🟢 SSL 핸드셰이크 성공: ${call.request().url.host} (${handshake.cipherSuite})")
        }
    }

    override fun callFailed(call: Call, ioe: IOException) {
        Timber.e(ioe, "🔴 호출 실패: ${call.request().url.redactSecrets()} - ${ioe.message}")
    }
}

// call.request()는 인터셉터가 키를 주입하기 전의 원본 요청이라 주입 키는 없지만,
// 호출부가 @Query로 직접 붙이는 session_id는 들어 있다. Timber.e는 파일 로그(공유 가능)에도 남으므로
// HttpLoggingInterceptor.redactQueryParams와 같은 목록·같은 마스크("██")로 가린다.
private fun HttpUrl.redactSecrets(): String {
    val builder = newBuilder()
    SecretQueryParams.ALL
        .filter { queryParameter(it) != null }
        .forEach { builder.setQueryParameter(it, "██") }
    return builder.build().toString()
}
