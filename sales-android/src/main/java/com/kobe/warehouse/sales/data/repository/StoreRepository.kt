package com.kobe.warehouse.sales.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.kobe.warehouse.sales.data.api.StoreApiService
import com.kobe.warehouse.sales.data.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Store Repository
 * Handles store information with caching
 */
class StoreRepository(
    private val storeApiService: StoreApiService,
    private val context: Context
) {
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "store_cache"
        private const val KEY_STORE_DATA = "store_data"
        private const val KEY_CACHE_TIME = "cache_time"
        private const val CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Get store information (with caching)
     */
    suspend fun getStore(forceRefresh: Boolean = false): Result<Store> {
        return withContext(Dispatchers.IO) {
            // Check cache first
            if (!forceRefresh) {
                val cachedStore = getCachedStore()
                if (cachedStore != null && isCacheValid()) {
                    return@withContext Result.success(cachedStore)
                }
            }

            // Fetch from API
            try {
                val response = storeApiService.getCurrentUserStore()
                if (response.isSuccessful && response.body() != null) {
                    val store = response.body()!!
                    // Save to cache
                    saveToCache(store)
                    Result.success(store)
                } else {
                    // Return cached data if API fails
                    val cachedStore = getCachedStore()
                    if (cachedStore != null) {
                        Result.success(cachedStore)
                    } else {
                        Result.failure(Exception("Failed to load store info: ${response.message()}"))
                    }
                }
            } catch (e: Exception) {
                // Return cached data if network error
                val cachedStore = getCachedStore()
                if (cachedStore != null) {
                    Result.success(cachedStore)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Get cached store data
     */
    private fun getCachedStore(): Store? {
        val storeJson = prefs.getString(KEY_STORE_DATA, null)
        return if (storeJson != null) {
            try {
                gson.fromJson(storeJson, Store::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Check if cache is still valid
     */
    private fun isCacheValid(): Boolean {
        val cacheTime = prefs.getLong(KEY_CACHE_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - cacheTime) < CACHE_VALIDITY_MS
    }

    /**
     * Save store to cache
     */
    private fun saveToCache(store: Store) {
        val storeJson = gson.toJson(store)
        prefs.edit()
            .putString(KEY_STORE_DATA, storeJson)
            .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        prefs.edit()
            .remove(KEY_STORE_DATA)
            .remove(KEY_CACHE_TIME)
            .apply()
    }
}
