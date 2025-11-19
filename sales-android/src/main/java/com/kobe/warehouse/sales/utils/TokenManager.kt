package com.kobe.warehouse.sales.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kobe.warehouse.sales.data.model.ServerConfig
import com.kobe.warehouse.sales.data.model.auth.JwtTokenResponse

/**
 * Token Manager for secure JWT token storage
 * Uses EncryptedSharedPreferences for security
 * Following the same logic as web's JwtTokenService
 */
class TokenManager(context: Context) {

    companion object {
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
        private const val AUTHORITY_DELIMITER = ","
    }

    private val sharedPreferences: SharedPreferences

    init {
        // Create master key for encryption
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Create encrypted shared preferences
        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Store JWT tokens from login response
     */
    fun storeTokens(tokenResponse: JwtTokenResponse) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            putString(KEY_TOKEN_TYPE, tokenResponse.tokenType)
            putLong(KEY_EXPIRES_IN, tokenResponse.expiresIn)
            putLong(KEY_EXPIRATION_TIMESTAMP, tokenResponse.getExpirationTimestamp())
            apply()
        }
    }

    /**
     * Get access token
     */
    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Get refresh token
     */
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Get token type (usually "Bearer")
     */
    fun getTokenType(): String {
        return sharedPreferences.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
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
        val expirationTimestamp = sharedPreferences.getLong(KEY_EXPIRATION_TIMESTAMP, 0)
        return System.currentTimeMillis() >= expirationTimestamp
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
        sharedPreferences.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_TYPE)
            remove(KEY_EXPIRES_IN)
            remove(KEY_EXPIRATION_TIMESTAMP)
            apply()
        }
    }

    /**
     * Store remember me preference and credentials
     */
    fun storeRememberMe(username: String, password: String, rememberMe: Boolean) {
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
    }

    /**
     * Get remember me preference
     */
    fun getRememberMe(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }

    /**
     * Get saved username
     */
    fun getSavedUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    /**
     * Get saved password
     */
    fun getSavedPassword(): String? {
        return sharedPreferences.getString(KEY_PASSWORD, null)
    }

    /**
     * Clear all data (logout completely)
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Save server configuration
     */
    fun saveServerConfig(config: ServerConfig) {
        sharedPreferences.edit().apply {
            putString(KEY_SERVER_URL, config.serverUrl)
            putString(KEY_SERVER_PROTOCOL, config.protocol)
            putString(KEY_SERVER_HOST, config.host)
            putString(KEY_SERVER_PORT, config.port)
            putInt(KEY_RECEIPT_ROLL_SIZE, config.receiptRollSize)
            apply()
        }
    }

    /**
     * Get server configuration
     */
    fun getServerConfig(): ServerConfig {
        val serverUrl = sharedPreferences.getString(KEY_SERVER_URL, null)
        val rollSize = sharedPreferences.getInt(KEY_RECEIPT_ROLL_SIZE, 58)

        if (serverUrl != null) {
            val protocol = sharedPreferences.getString(KEY_SERVER_PROTOCOL, "http") ?: "http"
            val host = sharedPreferences.getString(KEY_SERVER_HOST, "10.0.2.2") ?: "10.0.2.2"
            val port = sharedPreferences.getString(KEY_SERVER_PORT, "9080") ?: "9080"
            return ServerConfig(serverUrl, protocol, host, port, rollSize)
        }
        return ServerConfig.default()
    }

    /**
     * Get receipt roll size
     */
    fun getReceiptRollSize(): Int {
        return sharedPreferences.getInt(KEY_RECEIPT_ROLL_SIZE, 58)
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
        sharedPreferences.edit().apply {
            if (authorities.isNullOrEmpty()) {
                remove(KEY_USER_AUTHORITIES)
            } else {
                putString(KEY_USER_AUTHORITIES, authorities.joinToString(AUTHORITY_DELIMITER))
            }
            apply()
        }
    }

    /**
     * Get stored user authorities
     */
    fun getAuthorities(): List<String> {
        val authoritiesString = sharedPreferences.getString(KEY_USER_AUTHORITIES, null)
        return if (authoritiesString.isNullOrEmpty()) {
            emptyList()
        } else {
            authoritiesString.split(AUTHORITY_DELIMITER)
        }
    }

    /**
     * Check if user has specific authority
     */
    fun hasAuthority(authority: String): Boolean {
        return getAuthorities().contains(authority)
    }
}
