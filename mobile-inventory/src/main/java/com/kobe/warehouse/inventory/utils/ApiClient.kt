package com.kobe.warehouse.inventory.utils

import android.app.Application
import android.content.Context
import com.kobe.warehouse.inventory.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Retrofit API Client Factory
 * Creates Retrofit instances with JWT token authentication and error handling
 */
object ApiClient {

    private const val DEFAULT_BASE_URL = BuildConfig.BASE_URL

    /**
     * Typé `Application` et non `Context` : un `Context` retenu dans un champ statique
     * est signalé comme fuite mémoire (lint `StaticFieldLeak`). L'instance
     * d'`Application` vit aussi longtemps que le processus, elle ne peut rien retenir
     * d'autre.
     */
    private var application: Application? = null

    fun init(context: Context) {
        application = context.applicationContext as? Application
    }

    fun create(baseUrl: String? = null, tokenManager: TokenManager): Retrofit {
        val tokenManagerUrl = tokenManager.getBaseUrl()
        val finalBaseUrl = baseUrl
            ?: tokenManagerUrl.takeIf { it.isNotEmpty() }
            ?: DEFAULT_BASE_URL

        return Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(createOkHttpClient(tokenManager, finalBaseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun createOkHttpClient(tokenManager: TokenManager, baseUrl: String): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(createAuthInterceptor(tokenManager))
            .addInterceptor(createResponseInterceptor())
            .addInterceptor(createLoggingInterceptor())
            // Renouvelle le token expiré et rejoue la requête, plutôt que de couper
            // l'opérateur en plein comptage
            .authenticator(TokenRefreshAuthenticator(tokenManager, baseUrl))
            .build()
    }

    private fun createResponseInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()

            try {
                val response = chain.proceed(request)

                // 401 : l'Authenticator tente d'abord un renouvellement silencieux.
                // On ne notifie la fin de session que si la requête rejouée échoue
                // encore (priorResponse non nul = tentative déjà faite).
                if (response.code == 401 && response.priorResponse != null) {
                    application?.let { context ->
                        val sessionManager = SessionManager.getInstance(context)
                        sessionManager.handleUnauthorized("Session expirée, reconnexion nécessaire")
                    }
                }

                response
            } catch (e: IOException) {
                application?.let { context ->
                    val sessionManager = SessionManager.getInstance(context)
                    when (e) {
                        is SocketTimeoutException -> {
                            sessionManager.handleConnectionLost("Délai de connexion dépassé")
                        }
                        is UnknownHostException -> {
                            sessionManager.handleConnectionLost("Serveur inaccessible")
                        }
                        else -> {
                            sessionManager.handleConnectionLost("Erreur de connexion")
                        }
                    }
                }
                throw e
            }
        }
    }

    private fun createAuthInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            val authHeader = tokenManager.getAuthorizationHeader()
            if (!authHeader.isNullOrEmpty()) {
                requestBuilder.header("Authorization", authHeader)
            }

            requestBuilder
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-PHARMA-SMART-INVENTORY", "true")
                .method(original.method, original.body)

            chain.proceed(requestBuilder.build())
        }
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
}
