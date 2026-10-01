package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.model.NetworkStatus
import com.example.model.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

class NetworkMonitor(private val context: Context) {

    private val tag = "AivoraNetworkMonitor"
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _networkStatus = MutableStateFlow(getCurrentNetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(tag, "Network onAvailable: $network")
            updateStatus()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(tag, "Network onLost: $network")
            updateStatus()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateStatus()
        }
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
            updateStatus()
        } catch (e: Exception) {
            Log.e(tag, "Failed to register network callback", e)
        }
    }

    fun updateStatus() {
        val status = getCurrentNetworkStatus()
        _networkStatus.value = status
    }

    fun getCurrentNetworkStatus(): NetworkStatus {
        val cm = connectivityManager ?: return NetworkStatus(
            isConnected = false,
            networkType = NetworkType.NONE,
            interfaceDetails = "ConnectivityManager unavailable"
        )

        val activeNetwork = cm.activeNetwork
        if (activeNetwork == null) {
            return NetworkStatus(
                isConnected = false,
                networkType = NetworkType.NONE,
                interfaceDetails = "No active network interface"
            )
        }

        val capabilities = cm.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            return NetworkStatus(
                isConnected = false,
                networkType = NetworkType.NONE,
                interfaceDetails = "No network capabilities found"
            )
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }

        // True if network has internet capability (and preferably validated)
        val connected = hasInternet

        val details = buildString {
            append(type.name)
            if (isValidated) append(" (Validated)") else append(" (Unvalidated)")
            if (isMetered) append(" [Metered]")
        }

        return NetworkStatus(
            isConnected = connected,
            networkType = type,
            isMetered = isMetered,
            hasInternetCapability = hasInternet,
            lastCheckedTimestamp = System.currentTimeMillis(),
            interfaceDetails = details
        )
    }

    suspend fun verifyDnsConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Test lookup for reliable global host
            val address = InetAddress.getByName("api.binance.com")
            address != null && !address.hostAddress.isNullOrEmpty()
        } catch (e: Exception) {
            Log.w(tag, "DNS resolution test failed: ${e.message}")
            false
        }
    }
}
