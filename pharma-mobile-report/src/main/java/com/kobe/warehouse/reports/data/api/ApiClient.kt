package com.kobe.warehouse.reports.data.api

import com.kobe.warehouse.reports.BuildConfig
import com.kobe.warehouse.reports.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton API client factory.
 * Creates Retrofit instance with proper authentication and error handling.
 */
object ApiClient {

    private const val TIMEOUT_SECONDS = 30L

    /**
     * Create a configured Retrofit instance.
     *
     * @param baseUrl Optional base URL (uses TokenManager config or BuildConfig default)
     * @param tokenManager Token manager for JWT authentication
     * @return Configured Retrofit instance
     */
    fun create(
        baseUrl: String? = null,
        tokenManager: TokenManager
    ): Retrofit {
        val finalBaseUrl = baseUrl
            ?: tokenManager.getBaseUrl()
            ?: BuildConfig.BASE_URL

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
            val response = chain.proceed(request)

            // Handle 401 Unauthorized
            if (response.code == 401) {
                tokenManager.clearTokens()
                // SessionManager will handle redirect to login
            }

            response
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
