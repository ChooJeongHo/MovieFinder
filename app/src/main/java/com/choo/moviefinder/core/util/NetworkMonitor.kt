package com.choo.moviefinder.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * 실시간 네트워크 연결 상태를 모니터링하는 클래스.
 * ConnectivityManager.NetworkCallback을 사용하여 온/오프라인 변화를 감지하고,
 * StateFlow로 현재 연결 상태를 노출한다.
 *
 * Hilt @Singleton 스코프로 ApplicationContext와 함께 사용되므로
 * 앱 프로세스 종료 시 OS가 자동 정리한다. [unregister]는 테스트 정리용으로 제공.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(checkCurrentConnectivity())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // 네트워크 연결/해제/변경 이벤트를 수신하는 콜백
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            _isConnected.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            _isConnected.value =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    init {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: SecurityException) {
            Timber.w(e, "ACCESS_NETWORK_STATE 권한 없음, 네트워크 모니터링 비활성화")
        }
    }

    /** 네트워크 콜백 해제 (테스트 정리용, 프로덕션에서는 앱 프로세스 종료 시 OS가 자동 정리) */
    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "이미 해제된 네트워크 콜백")
        }
    }

    /** 현재 네트워크 연결 상태를 동기적으로 확인 (초기값 설정용) */
    private fun checkCurrentConnectivity(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: SecurityException) {
            Timber.w(e, "ACCESS_NETWORK_STATE 권한 없음")
            false
        }
    }
}
