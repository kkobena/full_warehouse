package com.kobe.warehouse.sales.data.model

/**
 * Server configuration model
 * Stores backend server URL, port, and receipt printer settings
 */
data class ServerConfig(
    val serverUrl: String = "http://10.0.2.2:9080",
    val protocol: String = "http",
    val host: String = "10.0.2.2",
    val port: String = "9080",
    val receiptRollSize: Int = 58  // Receipt paper roll size in mm (58mm or 80mm)
) {
    /**
     * Get full base URL for API
     */
    fun getBaseUrl(): String {
        return "$protocol://$host:$port/"
    }

    /**
     * Get auth endpoint
     */
    fun getAuthEndpoint(): String {
        return "${getBaseUrl()}api/auth/login"
    }

    /**
     * Get account endpoint
     */
    fun getAccountEndpoint(): String {
        return "${getBaseUrl()}api/account"
    }

    companion object {
        /**
         * Parse server URL into components
         */
        fun fromUrl(url: String): ServerConfig {
            val cleanUrl = url.trim().removeSuffix("/")

            val protocol = when {
                cleanUrl.startsWith("https://") -> "https"
                cleanUrl.startsWith("http://") -> "http"
                else -> "http"
            }

            val withoutProtocol = cleanUrl
                .removePrefix("https://")
                .removePrefix("http://")

            val parts = withoutProtocol.split(":")
            val host = parts.getOrNull(0) ?: "10.0.2.2"  // Default to emulator host
            val port = parts.getOrNull(1) ?: "9080"

            return ServerConfig(
                serverUrl = "$protocol://$host:$port",
                protocol = protocol,
                host = host,
                port = port
            )
        }

        /**
         * Create from components
         */
        fun create(protocol: String, host: String, port: String, receiptRollSize: Int = 58): ServerConfig {
            return ServerConfig(
                serverUrl = "$protocol://$host:$port",
                protocol = protocol,
                host = host,
                port = port,
                receiptRollSize = receiptRollSize
            )
        }

        /**
         * Default configuration
         */
        fun default(): ServerConfig {
            return ServerConfig()
        }
    }
}
