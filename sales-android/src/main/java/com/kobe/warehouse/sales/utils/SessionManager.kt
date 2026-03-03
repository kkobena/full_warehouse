package com.kobe.warehouse.sales.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Session Manager
 * Handles global session state and authentication events
 * Uses SharedFlow to emit session events to observers
 */
class SessionManager(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Session event types
     */
    enum class SessionEvent {
        SESSION_EXPIRED,
        UNAUTHORIZED,
        CONNECTION_LOST
    }

    private val tokenManager = TokenManager(context)
    private val _sessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    /**
     * Handle session expiration
     * Clears tokens and emits session expired event
     */
    fun handleSessionExpired(reason: String = "Session expired") {
        clearSession()
        _sessionEvents.tryEmit(SessionEvent.SESSION_EXPIRED)
    }

    /**
     * Handle unauthorized error (401)
     */
    fun handleUnauthorized(reason: String = "Unauthorized access") {
        clearSession()
        _sessionEvents.tryEmit(SessionEvent.UNAUTHORIZED)
    }

    /**
     * Handle connection lost
     * Clears session and emits connection lost event
     */
    fun handleConnectionLost(reason: String = "Connection lost") {
        clearSession()
        _sessionEvents.tryEmit(SessionEvent.CONNECTION_LOST)
    }

    /**
     * Clear session data
     */
    private fun clearSession() {
        tokenManager.clearTokens()
    }
}
