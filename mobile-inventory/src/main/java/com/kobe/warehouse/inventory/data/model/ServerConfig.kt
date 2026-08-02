package com.kobe.warehouse.inventory.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration du serveur backend.
 *
 * Aligné sur le module de vente (`sales-android`) : mêmes composants, mêmes valeurs par
 * défaut, même port — le backend embarqué dans Tauri écoute sur **9080**
 * (`config.server.port`, cf. `backend_manager.rs`).
 */
@Parcelize
data class ServerConfig(
    val serverUrl: String = "http://10.0.2.2:9080",
    val protocol: String = "http",
    val host: String = "10.0.2.2",
    val port: String = "9080"
) : Parcelable {

    /** URL de base de l'API, terminée par `/` (exigé par Retrofit) */
    fun getBaseUrl(): String = "$protocol://$host:$port/"

    fun getAuthEndpoint(): String = "${getBaseUrl()}api/auth/login"

    fun getAccountEndpoint(): String = "${getBaseUrl()}api/account"

    companion object {
        const val DEFAULT_HOST = "10.0.2.2"
        const val DEFAULT_PORT = "9080"

        /** Analyse une URL complète et en extrait les composants */
        fun fromUrl(url: String): ServerConfig {
            val cleanUrl = url.trim().removeSuffix("/")

            val protocol = when {
                cleanUrl.startsWith("https://") -> "https"
                else -> "http"
            }

            val withoutProtocol = cleanUrl
                .removePrefix("https://")
                .removePrefix("http://")

            val parts = withoutProtocol.split(":")
            val host = parts.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: DEFAULT_HOST
            val port = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: DEFAULT_PORT

            return ServerConfig("$protocol://$host:$port", protocol, host, port)
        }

        /** Construit une configuration à partir de ses composants */
        fun create(protocol: String, host: String, port: String): ServerConfig =
            ServerConfig("$protocol://$host:$port", protocol, host, port)

        fun default(): ServerConfig = ServerConfig()
    }
}
