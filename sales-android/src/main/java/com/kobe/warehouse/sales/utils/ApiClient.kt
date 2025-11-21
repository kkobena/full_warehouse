package com.kobe.warehouse.sales.utils

import android.content.Context
import com.kobe.warehouse.sales.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
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
    private var applicationContext: Context? = null

    /**
     * Initialize ApiClient with application context
     * This is needed for SessionManager to broadcast events
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * Create Retrofit instance with JWT authentication
     *
     * @param baseUrl API base URL (default: from BuildConfig or TokenManager)
     * @param tokenManager Token manager for JWT tokens and server config
     * @return Retrofit instance
     */
    fun create(baseUrl: String? = null, tokenManager: TokenManager): Retrofit {
        // Use base URL from: 1) parameter, 2) TokenManager config, 3) BuildConfig default
        val tokenManagerUrl = tokenManager.getBaseUrl()
        val finalBaseUrl = baseUrl
            ?: tokenManagerUrl.takeIf { it.isNotEmpty() }
            ?: DEFAULT_BASE_URL

        return Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(createOkHttpClient(tokenManager))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Create OkHttpClient with interceptors
     */
    private fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(createAuthInterceptor(tokenManager))
            .addInterceptor(createResponseInterceptor())
            .addInterceptor(createLoggingInterceptor())
            .build()
    }

    /**
     * Create response interceptor
     * Handles 401 Unauthorized and network errors
     */
    private fun createResponseInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()

            try {
                val response = chain.proceed(request)

                // Check for 401 Unauthorized
                if (response.code == 401) {
                    applicationContext?.let { context ->
                        val sessionManager = SessionManager.getInstance(context)
                        sessionManager.handleUnauthorized("Token expired or invalid")
                    }
                }

                response
            } catch (e: IOException) {
                // Handle network errors - redirect to login
                applicationContext?.let { context ->
                    val sessionManager = SessionManager.getInstance(context)
                    when (e) {
                        is SocketTimeoutException -> {
                            sessionManager.handleConnectionLost("Délai de connexion dépassé. Veuillez vérifier le serveur.")
                        }
                        is UnknownHostException -> {
                            sessionManager.handleConnectionLost("Serveur inaccessible. Veuillez vérifier la configuration.")
                        }
                        else -> {
                            sessionManager.handleConnectionLost("Erreur de connexion au serveur.")
                        }
                    }
                }
                throw e
            }
        }
    }

    /**
     * Create authentication interceptor
     * Adds Authorization: Bearer {token} header to all requests
     * Adds custom x-PHARMA-SMART-ANDROID header to identify Android client
     * Same logic as web's HTTP interceptor
     */
    private fun createAuthInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            // Add Authorization header if token exists
            val authHeader = tokenManager.getAuthorizationHeader()
            if (!authHeader.isNullOrEmpty()) {
                requestBuilder.header("Authorization", authHeader)
            }

            // Add common headers
            requestBuilder
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-PHARMA-SMART-ANDROID", "true")  // Custom header to identify Android client
                .method(original.method, original.body)

            chain.proceed(requestBuilder.build())
        }
    }

    /**
     * Create logging interceptor for debugging
     * Only enabled in debug builds
     */
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
