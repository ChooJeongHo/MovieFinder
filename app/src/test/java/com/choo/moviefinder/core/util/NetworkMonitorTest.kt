package com.choo.moviefinder.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var network: Network
    private lateinit var capabilities: NetworkCapabilities
    private val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        network = mockk(relaxed = true)
        capabilities = mockk(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just runs
    }

    private fun buildMonitorWithConnectivity(connected: Boolean): NetworkMonitor {
        if (connected) {
            every { connectivityManager.activeNetwork } returns network
            every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
            every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        } else {
            every { connectivityManager.activeNetwork } returns null
        }
        return NetworkMonitor(context)
    }

    @Test
    fun `initial state - connected when network available`() {
        val monitor = buildMonitorWithConnectivity(connected = true)
        assertTrue(monitor.isConnected.value)
    }

    @Test
    fun `initial state - disconnected when no active network`() {
        val monitor = buildMonitorWithConnectivity(connected = false)
        assertFalse(monitor.isConnected.value)
    }

    @Test
    fun `onAvailable callback - sets isConnected to true`() = runTest {
        val monitor = buildMonitorWithConnectivity(connected = false)
        assertFalse(monitor.isConnected.value)

        callbackSlot.captured.onAvailable(network)

        assertTrue(monitor.isConnected.value)
    }

    @Test
    fun `onLost callback - sets isConnected to false`() = runTest {
        val monitor = buildMonitorWithConnectivity(connected = true)
        assertTrue(monitor.isConnected.value)

        callbackSlot.captured.onLost(network)

        assertFalse(monitor.isConnected.value)
    }

    @Test
    fun `onCapabilitiesChanged - with internet capability sets connected true`() = runTest {
        val monitor = buildMonitorWithConnectivity(connected = false)
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        callbackSlot.captured.onCapabilitiesChanged(network, capabilities)

        assertTrue(monitor.isConnected.value)
    }

    @Test
    fun `onCapabilitiesChanged - without internet capability sets connected false`() = runTest {
        val monitor = buildMonitorWithConnectivity(connected = true)
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        callbackSlot.captured.onCapabilitiesChanged(network, capabilities)

        assertFalse(monitor.isConnected.value)
    }

    @Test
    fun `SecurityException during init - initializes with false and does not crash`() {
        every { connectivityManager.activeNetwork } throws SecurityException("no permission")
        every { connectivityManager.registerDefaultNetworkCallback(any()) } just runs

        val monitor = NetworkMonitor(context)

        assertFalse(monitor.isConnected.value)
    }

    @Test
    fun `unregister - calls unregisterNetworkCallback`() {
        val monitor = buildMonitorWithConnectivity(connected = true)
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just runs

        monitor.unregister()

        verify(exactly = 1) {
            connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>())
        }
    }
}
