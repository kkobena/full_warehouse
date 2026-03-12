package com.kobe.warehouse.sales.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kobe.warehouse.sales.data.model.ServerConfig
import com.kobe.warehouse.sales.data.model.auth.JwtTokenResponse
import androidx.core.content.edit
import java.security.GeneralSecurityException

/**
 * Token Manager for secure JWT token storage
 * Uses EncryptedSharedPreferences for security
 * Following the same logic as web's JwtTokenService
 */
class TokenManager(context: Context) {

    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "pharma_smart_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_EXPIRATION_TIMESTAMP = "expiration_timestamp"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SERVER_PROTOCOL = "server_protocol"
        private const val KEY_SERVER_HOST = "server_host"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_RECEIPT_ROLL_SIZE = "receipt_roll_size"
        private const val KEY_USER_AUTHORITIES = "user_authorities"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val AUTHORITY_DELIMITER = ","
    }

    private var sharedPreferences: SharedPreferences

    init {
        sharedPreferences = try {
            createEncryptedSharedPreferences(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, clearing and retrying", e)
            // If creation fails (e.g., AEADBadTagException), clear the preferences and try again
            clearEncryptedSharedPreferences(context)
            createEncryptedSharedPreferences(context)
        }
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        // Create master key for encryption
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Create encrypted shared preferences
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun clearEncryptedSharedPreferences(context: Context) {
        try {
            // Delete the shared preferences file
            val sharedPrefsFile = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefsFile.edit().clear().apply()

            // On some versions of Android, we might also need to delete the file manually
            // or use context.deleteSharedPreferences(PREFS_NAME) if available (API 24+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(PREFS_NAME)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear corrupted SharedPreferences", e)
        }
    }

    /**
     * Store JWT tokens from login response
     */
    fun storeTokens(tokenResponse: JwtTokenResponse) {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
                putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
                putString(KEY_TOKEN_TYPE, tokenResponse.tokenType)
                putLong(KEY_EXPIRES_IN, tokenResponse.expiresIn)
                putLong(KEY_EXPIRATION_TIMESTAMP, tokenResponse.getExpirationTimestamp())
                apply()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Get access token
     */
    fun getAccessToken(): String? {
        return try {
            sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    /**
     * Get refresh token
     */
    fun getRefreshToken(): String? {
        return try {
            sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    /**
     * Get token type (usually "Bearer")
     */
    fun getTokenType(): String {
        return try {
            sharedPreferences.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
        } catch (e: Exception) {
            handleException(e)
            "Bearer"
        }
    }

    /**
     * Get authorization header value
     * Format: "Bearer {accessToken}"
     */
    fun getAuthorizationHeader(): String? {
        val accessToken = getAccessToken() ?: return null
        return "${getTokenType()} $accessToken"
    }

    /**
     * Check if token is expired
     */
    fun isTokenExpired(): Boolean {
        return try {
            val expirationTimestamp = sharedPreferences.getLong(KEY_EXPIRATION_TIMESTAMP, 0)
            System.currentTimeMillis() >= expirationTimestamp
        } catch (e: Exception) {
            handleException(e)
            true
        }
    }

    /**
     * Check if user is authenticated (has valid token)
     */
    fun isAuthenticated(): Boolean {
        val accessToken = getAccessToken()
        return !accessToken.isNullOrEmpty() && !isTokenExpired()
    }

    /**
     * Clear all tokens (logout)
     */
    fun clearTokens() {
        try {
            sharedPreferences.edit().apply {
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_TOKEN_TYPE)
                remove(KEY_EXPIRES_IN)
                remove(KEY_EXPIRATION_TIMESTAMP)
                apply()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Store remember me preference and credentials
     */
    fun storeRememberMe(username: String, password: String, rememberMe: Boolean) {
        try {
            sharedPreferences.edit().apply {
                putBoolean(KEY_REMEMBER_ME, rememberMe)
                if (rememberMe) {
                    putString(KEY_USERNAME, username)
                    putString(KEY_PASSWORD, password)
                } else {
                    remove(KEY_USERNAME)
                    remove(KEY_PASSWORD)
                }
                apply()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Get remember me preference
     */
    fun getRememberMe(): Boolean {
        return try {
            sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
        } catch (e: Exception) {
            handleException(e)
            false
        }
    }

    /**
     * Get saved username
     */
    fun getSavedUsername(): String? {
        return try {
            sharedPreferences.getString(KEY_USERNAME, null)
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    /**
     * Get saved password
     */
    fun getSavedPassword(): String? {
        return try {
            sharedPreferences.getString(KEY_PASSWORD, null)
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    /**
     * Clear all data (logout completely)
     */
    fun clearAll() {
        try {
            sharedPreferences.edit { clear() }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Save server configuration
     */
    fun saveServerConfig(config: ServerConfig) {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_SERVER_URL, config.serverUrl)
                putString(KEY_SERVER_PROTOCOL, config.protocol)
                putString(KEY_SERVER_HOST, config.host)
                putString(KEY_SERVER_PORT, config.port)
                putInt(KEY_RECEIPT_ROLL_SIZE, config.receiptRollSize)
                apply()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Get server configuration
     */
    fun getServerConfig(): ServerConfig {
        return try {
            val serverUrl = sharedPreferences.getString(KEY_SERVER_URL, null)
            val rollSize = sharedPreferences.getInt(KEY_RECEIPT_ROLL_SIZE, 58)

            if (serverUrl != null) {
                val protocol = sharedPreferences.getString(KEY_SERVER_PROTOCOL, "http") ?: "http"
                val host = sharedPreferences.getString(KEY_SERVER_HOST, "10.0.2.2") ?: "10.0.2.2"
                val port = sharedPreferences.getString(KEY_SERVER_PORT, "9080") ?: "9080"
                ServerConfig(serverUrl, protocol, host, port, rollSize)
            } else {
                ServerConfig.default()
            }
        } catch (e: Exception) {
            handleException(e)
            ServerConfig.default()
        }
    }

    /**
     * Get receipt roll size
     */
    fun getReceiptRollSize(): Int {
        return try {
            sharedPreferences.getInt(KEY_RECEIPT_ROLL_SIZE, 58)
        } catch (e: Exception) {
            handleException(e)
            58
        }
    }

    /**
     * Get base URL for API
     */
    fun getBaseUrl(): String {
        return getServerConfig().getBaseUrl()
    }

    /**
     * Store user authorities (permissions)
     */
    fun storeAuthorities(authorities: List<String>?) {
        try {
            sharedPreferences.edit().apply {
                if (authorities.isNullOrEmpty()) {
                    remove(KEY_USER_AUTHORITIES)
                } else {
                    putString(KEY_USER_AUTHORITIES, authorities.joinToString(AUTHORITY_DELIMITER))
                }
                apply()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Get stored user authorities
     */
    fun getAuthorities(): List<String> {
        return try {
            val authoritiesString = sharedPreferences.getString(KEY_USER_AUTHORITIES, null)
            if (authoritiesString.isNullOrEmpty()) {
                emptyList()
            } else {
                authoritiesString.split(AUTHORITY_DELIMITER)
            }
        } catch (e: Exception) {
            handleException(e)
            emptyList()
        }
    }

    /**
     * Check if user has specific authority
     */
    fun hasAuthority(authority: String): Boolean {
        return getAuthorities().contains(authority)
    }

    /**
     * Store user ID (cassierId/sellerId for sales)
     * Note: Backend uses Integer for user IDs
     */
    fun storeUserId(userId: Int?) {
        try {
            sharedPreferences.edit().apply {
                if (userId != null) {
                    putInt(KEY_USER_ID, userId)
                    Log.d(TAG, "User ID stored successfully: $userId")
                } else {
                    remove(KEY_USER_ID)
                    Log.d(TAG, "User ID removed (null)")
                }
                apply()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Get stored user ID
     * Returns null if not stored
     */
    fun getUserId(): Int? {
        return try {
            val containsKey = sharedPreferences.contains(KEY_USER_ID)
            if (containsKey) {
                sharedPreferences.getInt(KEY_USER_ID, 0)
            } else {
                null
            }
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    /**
     * Store user full name
     */
    fun storeUserFullName(fullName: String?) {
        try {
            sharedPreferences.edit().apply {
                if (fullName != null) {
                    putString(KEY_USER_FULL_NAME, fullName)
                } else {
                    remove(KEY_USER_FULL_NAME)
                }
                apply()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Get stored user full name
     */
    fun getUserFullName(): String? {
        return try {
            sharedPreferences.getString(KEY_USER_FULL_NAME, null)
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    /**
     * Handles exceptions that occur during SharedPreferences operations.
     * If a GeneralSecurityException (like AEADBadTagException) occurs,
     * it might mean the encryption keys are corrupted.
     */
    private fun handleException(e: Exception) {
        Log.e(TAG, "Exception during SharedPreferences operation", e)
        // We could potentially re-initialize here if needed, but for now just log it.
        // If it's a security exception, it will likely keep failing until the app is restarted
        // and init {} block runs again to clear the corrupted prefs.
    }
}
