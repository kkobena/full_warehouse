package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.RemiseApiService
import com.kobe.warehouse.sales.data.model.Remise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Remise Repository
 * Fetches and caches predefined discounts from backend
 */
class RemiseRepository(
    private val remiseApiService: RemiseApiService
) {
    private var cachedRemises: List<Remise>? = null

    /**
     * Get all enabled remises (cached)
     */
    suspend fun getRemises(forceRefresh: Boolean = false): Result<List<Remise>> {
        if (!forceRefresh && cachedRemises != null) {
            return Result.success(cachedRemises!!)
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = remiseApiService.getRemises()
                if (response.isSuccessful && response.body() != null) {
                    val enabledRemises = response.body()!!.filter { it.enable }
                    cachedRemises = enabledRemises
                    Result.success(enabledRemises)
                } else {
                    Result.failure(Exception("Erreur chargement remises"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Clear cache (e.g. on logout)
     */
    fun clearCache() {
        cachedRemises = null
    }
}
