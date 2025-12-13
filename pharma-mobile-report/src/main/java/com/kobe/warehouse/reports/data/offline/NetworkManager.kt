package com.kobe.warehouse.reports.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Manages network connectivity monitoring.
 */
class NetworkManager private constructor(context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    companion object {
        private const val TAG = "NetworkManager"

        @Volatile
        private var INSTANCE: NetworkManager? = null

        fun getInstance(context: Context): NetworkManager {
            return INSTANCE ?: synchronized(this) {
                val instance = NetworkManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Check if device is currently online.
     */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Check if device is on WiFi.
     */
    fun isOnWiFi(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Check if device is on mobile data.
     */
    fun isOnMobileData(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Get network type as string.
     */
    fun getNetworkType(): NetworkType {
        if (!isOnline()) return NetworkType.NONE

        return when {
            isOnWiFi() -> NetworkType.WIFI
            isOnMobileData() -> NetworkType.MOBILE
            else -> NetworkType.OTHER
        }
    }

    /**
     * Observe network connectivity changes.
     * Returns a Flow that emits true when online, false when offline.
     */
    fun observeNetworkConnectivity(): Flow<Boolean> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                networks.add(network)
                trySend(networks.isNotEmpty())
                Log.d(TAG, "Network available: $network")
            }

            override fun onLost(network: Network) {
                networks.remove(network)
                trySend(networks.isNotEmpty())
                Log.d(TAG, "Network lost: $network")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                if (hasInternet && isValidated) {
                    networks.add(network)
                } else {
                    networks.remove(network)
                }

                trySend(networks.isNotEmpty())
                Log.d(TAG, "Network capabilities changed: $network (online: ${networks.isNotEmpty()})")
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Send initial state
        trySend(isOnline())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(TAG, "Network callback unregistered")
        }
    }.distinctUntilChanged()

    /**
     * Observe network type changes.
     */
    fun observeNetworkType(): Flow<NetworkType> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getNetworkType())
            }

            override fun onLost(network: Network) {
                trySend(getNetworkType())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getNetworkType())
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Send initial state
        trySend(getNetworkType())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()

    enum class NetworkType {
        NONE,
        WIFI,
        MOBILE,
        OTHER
    }
}
