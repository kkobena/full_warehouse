package com.kobe.warehouse.inventory.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kobe.warehouse.inventory.sync.SyncManager

/**
 * Global connectivity monitor.
 * Exposes the online state as LiveData and triggers an immediate sync
 * of pending inventory lines whenever connectivity is regained.
 */
object NetworkMonitor {

    private const val TAG = "NetworkMonitor"

    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline

    fun isCurrentlyOnline(): Boolean = _isOnline.value ?: true

    fun init(context: Context) {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        _isOnline.postValue(hasInternet(cm))

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasOffline = _isOnline.value == false
                _isOnline.postValue(true)
                if (wasOffline) {
                    Log.d(TAG, "Connectivity regained — triggering sync of pending lines")
                    SyncManager.syncNow(appContext)
                }
            }

            override fun onLost(network: Network) {
                // onLost fires per network; only go offline if nothing else is up
                _isOnline.postValue(hasInternet(cm))
            }
        })
    }

    private fun hasInternet(cm: ConnectivityManager): Boolean {
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
