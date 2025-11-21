package com.kobe.warehouse.inventory.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Server Configuration
 * Stores backend server connection details
 */
@Parcelize
data class ServerConfig(
    val serverUrl: String,
    val protocol: String = "http",
    val host: String = "10.0.2.2",
    val port: String = "8080"
) : Parcelable {

    companion object {
        fun default(): ServerConfig {
            return ServerConfig(
                serverUrl = "http://10.0.2.2:8080/",
                protocol = "http",
                host = "10.0.2.2",
                port = "8080"
            )
        }
    }

    fun getBaseUrl(): String {
        return if (serverUrl.isNotEmpty()) {
            serverUrl
        } else {
            "$protocol://$host:$port/"
        }
    }
}
