package com.kobe.warehouse.reports.data.api

import android.content.Context
import android.content.Intent
import com.kobe.warehouse.reports.BuildConfig
import com.kobe.warehouse.reports.ui.activity.BaseActivity
import com.kobe.warehouse.reports.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Singleton API client factory.
 * Creates Retrofit instance with proper authentication and error handling.
 */
object ApiClient {

    private const val TIMEOUT_SECONDS = 30L

    private var applicationContext: Context? = null

    /**
     * Initialize with application context for broadcasting events.
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * Create a configured Retrofit instance.
     *
     * @param baseUrl Optional base URL (uses TokenManager config or BuildConfig default)
     * @param tokenManager Token manager for JWT authentication
     * @return Configured Retrofit instance
     * @throws IllegalStateException if no valid base URL is configured
     */
    fun create(
        baseUrl: String? = null,
        tokenManager: TokenManager
    ): Retrofit {
        val finalBaseUrl = baseUrl?.takeIf { it.isNotBlank() }
            ?: tokenManager.getBaseUrl()?.takeIf { it.isNotBlank() }
            ?: BuildConfig.BASE_URL.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("No base URL configured. Please configure the server URL in settings.")

        val okHttpClient = createOkHttpClient(tokenManager)

        return Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Create configured OkHttpClient with interceptors.
     */
    private fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(createAuthInterceptor(tokenManager))
            .addInterceptor(createErrorInterceptor(tokenManager))
            .addInterceptor(createLoggingInterceptor())
            .build()
    }

    /**
     * Create auth interceptor that adds JWT token to requests.
     */
    private fun createAuthInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()

            // Skip auth for login endpoint
            if (originalRequest.url.encodedPath.contains("auth/login")) {
                return@Interceptor chain.proceed(originalRequest)
            }

            val token = tokenManager.getAccessToken()

            val requestBuilder = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-PHARMA-SMART-ANDROID", "true")

            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }

    /**
     * Create error interceptor for handling auth errors and connection issues.
     */
    private fun createErrorInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            try {
                val response = chain.proceed(chain.request())

                // Handle 401 Unauthorized
                if (response.code == 401) {
                    tokenManager.clearTokens()
                    broadcastEvent(BaseActivity.ACTION_UNAUTHORIZED)
                }

                // Handle 403 Forbidden
                if (response.code == 403) {
                    broadcastEvent(BaseActivity.ACTION_SESSION_EXPIRED)
                }

                response
            } catch (e: UnknownHostException) {
                broadcastEvent(BaseActivity.ACTION_CONNECTION_LOST)
                throw e
            } catch (e: ConnectException) {
                broadcastEvent(BaseActivity.ACTION_CONNECTION_LOST)
                throw e
            } catch (e: SocketTimeoutException) {
                broadcastEvent(BaseActivity.ACTION_CONNECTION_LOST)
                throw e
            }
        }
    }

    /**
     * Broadcast an event to all registered receivers.
     */
    private fun broadcastEvent(action: String) {
        applicationContext?.let { context ->
            val intent = Intent(action)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
    }

    /**
     * Create logging interceptor for debug builds.
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
