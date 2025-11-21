package com.kobe.warehouse.sales.utils

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Session Manager
 * Handles global session state and authentication events
 * Broadcasts session expiration events to logged-in activities
 */
class SessionManager(private val context: Context) {

    companion object {
        const val ACTION_SESSION_EXPIRED = "com.kobe.warehouse.sales.SESSION_EXPIRED"
        const val ACTION_UNAUTHORIZED = "com.kobe.warehouse.sales.UNAUTHORIZED"
        const val ACTION_CONNECTION_LOST = "com.kobe.warehouse.sales.CONNECTION_LOST"

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

    /**
     * Broadcast session expired event
     * This will trigger logout across all activities
     */
    fun broadcastSessionExpired() {
        val intent = Intent(ACTION_SESSION_EXPIRED)
        localBroadcastManager.sendBroadcast(intent)
    }

    /**
     * Broadcast unauthorized event (401 error)
     */
    fun broadcastUnauthorized() {
        val intent = Intent(ACTION_UNAUTHORIZED)
        localBroadcastManager.sendBroadcast(intent)
    }

    /**
     * Broadcast connection lost event
     */
    fun broadcastConnectionLost() {
        val intent = Intent(ACTION_CONNECTION_LOST)
        localBroadcastManager.sendBroadcast(intent)
    }

    /**
     * Handle session expiration
     * Clears tokens and broadcasts logout event
     */
    fun handleSessionExpired(reason: String = "Session expired") {
        clearSession()
        broadcastSessionExpired()
    }

    /**
     * Handle unauthorized error (401)
     */
    fun handleUnauthorized(reason: String = "Unauthorized access") {
        clearSession()
        broadcastUnauthorized()
    }

    /**
     * Handle connection lost
     * Clears session and redirects to login
     * Backend service must be accessible for the app to function
     */
    fun handleConnectionLost(reason: String = "Connection lost") {
        clearSession()
        broadcastConnectionLost()
    }

    /**
     * Clear session data
     */
    private fun clearSession() {
        tokenManager.clearTokens()
    }

}
