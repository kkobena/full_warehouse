package com.kobe.warehouse.inventory.utils

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
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun create(baseUrl: String? = null, tokenManager: TokenManager): Retrofit {
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

    private fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(createAuthInterceptor(tokenManager))
            .addInterceptor(createResponseInterceptor())
            .addInterceptor(createLoggingInterceptor())
            .build()
    }

    private fun createResponseInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()

            try {
                val response = chain.proceed(request)

                if (response.code == 401) {
                    applicationContext?.let { context ->
                        val sessionManager = SessionManager.getInstance(context)
                        sessionManager.handleUnauthorized("Token expired or invalid")
                    }
                }

                response
            } catch (e: IOException) {
                applicationContext?.let { context ->
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
