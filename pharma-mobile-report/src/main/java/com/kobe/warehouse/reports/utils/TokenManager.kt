package com.kobe.warehouse.reports.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure token manager using EncryptedSharedPreferences.
 * Handles JWT tokens, user credentials, and server configuration.
 * Falls back to regular SharedPreferences if encryption fails (some Samsung devices).
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences = createSharedPreferences(context)

    companion object {
        private const val PREFS_NAME = "pharma_report_secure_prefs"
        private const val PREFS_NAME_FALLBACK = "pharma_report_prefs"
        private const val TAG = "TokenManager"

        // Token keys
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRATION_TIME = "expiration_time"
        private const val KEY_AUTHORITIES = "authorities"

        // Remember Me keys
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_USERNAME = "saved_username"
        private const val KEY_SAVED_PASSWORD = "saved_password"

        // Server config keys
        private const val KEY_BASE_URL = "base_url"

        // FCM keys
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_FCM_TOKEN_REGISTERED = "fcm_token_registered"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Create SharedPreferences with encryption fallback.
         * Some Samsung devices (S22, etc.) have issues with EncryptedSharedPreferences.
         */
        private fun createSharedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                // Fallback to regular SharedPreferences on devices where encryption fails
                android.util.Log.w(TAG, "EncryptedSharedPreferences failed, using fallback: ${e.message}")
                context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
            }
        }
    }

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    /**
     * Store JWT tokens after login.
     */
    fun storeTokens(accessToken: String, refreshToken: String?, expiresIn: Long) {
        val expirationTime = System.currentTimeMillis() + (expiresIn * 1000)

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRATION_TIME, expirationTime)
            apply()
        }
    }

    /**
     * Get access token if not expired.
     */
    fun getAccessToken(): String? {
        val expirationTime = prefs.getLong(KEY_EXPIRATION_TIME, 0)
        if (System.currentTimeMillis() > expirationTime) {
            return null
        }
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Get refresh token.
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Check if user is authenticated with valid token.
     */
    fun isAuthenticated(): Boolean {
        return getAccessToken() != null
    }

    /**
     * Clear all tokens (logout).
     */
    fun clearTokens() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRATION_TIME)
            remove(KEY_AUTHORITIES)
            apply()
        }
    }

    // =========================================================================
    // USER AUTHORITIES
    // =========================================================================

    /**
     * Store user authorities.
     */
    fun storeAuthorities(authorities: List<String>) {
        prefs.edit().apply {
            putString(KEY_AUTHORITIES, authorities.joinToString(","))
            apply()
        }
    }

    /**
     * Get user authorities.
     */
    fun getAuthorities(): List<String> {
        val authString = prefs.getString(KEY_AUTHORITIES, "") ?: ""
        return if (authString.isNotEmpty()) {
            authString.split(",")
        } else {
            emptyList()
        }
    }

    /**
     * Check if user has a specific authority.
     */
    fun hasAuthority(authority: String): Boolean {
        return getAuthorities().contains(authority)
    }

    /**
     * Check if user is admin.
     */
    fun isAdmin(): Boolean {
        return hasAuthority("ROLE_ADMIN")
    }

    // =========================================================================
    // REMEMBER ME
    // =========================================================================

    /**
     * Store "Remember Me" credentials.
     */
    fun storeRememberMe(username: String, password: String, enabled: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, enabled)
            if (enabled) {
                putString(KEY_SAVED_USERNAME, username)
                putString(KEY_SAVED_PASSWORD, password)
            } else {
                remove(KEY_SAVED_USERNAME)
                remove(KEY_SAVED_PASSWORD)
            }
            apply()
        }
    }

    /**
     * Check if "Remember Me" is enabled.
     */
    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    /**
     * Get saved username.
     */
    fun getSavedUsername(): String? {
        return prefs.getString(KEY_SAVED_USERNAME, null)
    }

    /**
     * Get saved password.
     */
    fun getSavedPassword(): String? {
        return prefs.getString(KEY_SAVED_PASSWORD, null)
    }

    // =========================================================================
    // SERVER CONFIGURATION
    // =========================================================================

    /**
     * Store custom base URL.
     */
    fun storeBaseUrl(baseUrl: String) {
        prefs.edit().apply {
            putString(KEY_BASE_URL, baseUrl)
            apply()
        }
    }

    /**
     * Get custom base URL (null if using default).
     */
    fun getBaseUrl(): String? {
        return prefs.getString(KEY_BASE_URL, null)
    }

    /**
     * Clear custom base URL (revert to default).
     */
    fun clearBaseUrl() {
        prefs.edit().apply {
            remove(KEY_BASE_URL)
            apply()
        }
    }

    /**
     * Save server URL (alias for storeBaseUrl).
     */
    fun saveServerUrl(url: String) {
        storeBaseUrl(url)
    }

    /**
     * Get server URL (returns empty string if not set).
     */
    fun getServerUrl(): String {
        return getBaseUrl() ?: ""
    }

    // =========================================================================
    // FCM TOKEN MANAGEMENT
    // =========================================================================

    /**
     * Save FCM token.
     */
    fun saveFcmToken(token: String) {
        prefs.edit().apply {
            putString(KEY_FCM_TOKEN, token)
            putBoolean(KEY_FCM_TOKEN_REGISTERED, false)
            apply()
        }
    }

    /**
     * Get FCM token.
     */
    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Mark FCM token as registered with backend.
     */
    fun setFcmTokenRegistered(registered: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_FCM_TOKEN_REGISTERED, registered)
            apply()
        }
    }

    /**
     * Check if FCM token is registered with backend.
     */
    fun isFcmTokenRegistered(): Boolean {
        return prefs.getBoolean(KEY_FCM_TOKEN_REGISTERED, false)
    }

    /**
     * Clear FCM token on logout.
     */
    fun clearFcmToken() {
        prefs.edit().apply {
            remove(KEY_FCM_TOKEN)
            remove(KEY_FCM_TOKEN_REGISTERED)
            apply()
        }
    }

    // =========================================================================
    // NOTIFICATION PREFERENCES
    // =========================================================================

    /**
     * Enable or disable notifications.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            apply()
        }
    }

    /**
     * Check if notifications are enabled.
     */
    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
}
