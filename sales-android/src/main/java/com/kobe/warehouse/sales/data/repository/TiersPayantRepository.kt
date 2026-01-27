package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.TiersPayantApiService
import com.kobe.warehouse.sales.domain.model.TiersPayant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tiers Payant Repository
 * Handles insurance provider data operations
 */
class TiersPayantRepository(
    private val tiersPayantApiService: TiersPayantApiService
) {

    // Cache active tiers payants
    private var cachedActiveTiersPayants: List<TiersPayant>? = null

    /**
     * Search tiers payants by name or code
     */
    suspend fun searchTiersPayants(query: String, size: Int = 50): Result<List<TiersPayant>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = tiersPayantApiService.searchTiersPayants(query, size)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.filter { it.enabled })
                } else {
                    Result.failure(Exception("Failed to search tiers payants: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get tiers payant by ID
     */
    suspend fun getTiersPayantById(id: Long): Result<TiersPayant> {
        return withContext(Dispatchers.IO) {
            try {
                val response = tiersPayantApiService.getTiersPayantById(id)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Tiers payant not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get all active tiers payants
     */
    suspend fun getActiveTiersPayants(forceRefresh: Boolean = false): Result<List<TiersPayant>> {
        return withContext(Dispatchers.IO) {
            try {
                // Return cached if available
                if (!forceRefresh && cachedActiveTiersPayants != null) {
                    return@withContext Result.success(cachedActiveTiersPayants!!)
                }

                val response = tiersPayantApiService.getActiveTiersPayants()
                if (response.isSuccessful && response.body() != null) {
                    cachedActiveTiersPayants = response.body()!!
                    Result.success(cachedActiveTiersPayants!!)
                } else {
                    Result.failure(Exception("Failed to load active tiers payants: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get tiers payants for a specific customer
     */
    suspend fun getCustomerTiersPayants(customerId: Long): Result<List<TiersPayant>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = tiersPayantApiService.getCustomerTiersPayants(customerId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load customer tiers payants: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Clear cached data
     */
    fun clearCache() {
        cachedActiveTiersPayants = null
    }
}
