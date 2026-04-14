package com.choo.moviefinder.core.util

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
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
        Timber.e(ioe, "🔴 호출 실패: ${call.request().url} - ${ioe.message}")
    }
}
