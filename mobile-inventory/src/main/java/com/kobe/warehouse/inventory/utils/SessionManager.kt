package com.kobe.warehouse.inventory.utils

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Session Manager
 * Handles global session state and authentication events
 */
class SessionManager(private val context: Context) {

    companion object {
        const val ACTION_SESSION_EXPIRED = "com.kobe.warehouse.inventory.SESSION_EXPIRED"
        const val ACTION_UNAUTHORIZED = "com.kobe.warehouse.inventory.UNAUTHORIZED"
        const val ACTION_CONNECTION_LOST = "com.kobe.warehouse.inventory.CONNECTION_LOST"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val tokenManager = TokenManager(context)
    private val localBroadcastManager = LocalBroadcastManager.getInstance(context)

    fun broadcastSessionExpired() {
        val intent = Intent(ACTION_SESSION_EXPIRED)
        localBroadcastManager.sendBroadcast(intent)
    }

    fun broadcastUnauthorized() {
        val intent = Intent(ACTION_UNAUTHORIZED)
        localBroadcastManager.sendBroadcast(intent)
    }

    fun broadcastConnectionLost() {
        val intent = Intent(ACTION_CONNECTION_LOST)
        localBroadcastManager.sendBroadcast(intent)
    }

    fun handleSessionExpired(reason: String = "Session expired") {
        clearSession()
        broadcastSessionExpired()
    }

    fun handleUnauthorized(reason: String = "Unauthorized access") {
        clearSession()
        broadcastUnauthorized()
    }

    fun handleConnectionLost(reason: String = "Connection lost") {
        clearSession()
        broadcastConnectionLost()
    }

    private fun clearSession() {
        tokenManager.clearTokens()
    }
}
