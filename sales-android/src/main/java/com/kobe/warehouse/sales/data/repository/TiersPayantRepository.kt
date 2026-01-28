package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.TiersPayantApiService
import com.kobe.warehouse.sales.domain.model.TiersPayant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for Tiers Payant (Insurance Provider) operations
 */
class TiersPayantRepository(
    private val tiersPayantApi: TiersPayantApiService
) {

    /**
     * Get all active tiers payants
     */
    suspend fun getAllTiersPayants(): Result<List<TiersPayant>> = withContext(Dispatchers.IO) {
        try {
            val response = tiersPayantApi.getAllTiersPayants()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search tiers payants by name or code
     */
    suspend fun searchTiersPayants(query: String): Result<List<TiersPayant>> = withContext(Dispatchers.IO) {
        try {
            val response = tiersPayantApi.searchTiersPayants(query)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get tiers payant by ID
     */
    suspend fun getTiersPayantById(id: Long): Result<TiersPayant> = withContext(Dispatchers.IO) {
        try {
            val response = tiersPayantApi.getTiersPayantById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get tiers payants for a specific customer
     * Returns the customer's principal and complementaire insurance providers
     */
    suspend fun getTiersPayantsForCustomer(customerId: Long): Result<List<TiersPayant>> = withContext(Dispatchers.IO) {
        try {
            val response = tiersPayantApi.getTiersPayantsForCustomer(customerId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate if a tiers payant is valid for a customer
     * Checks:
     * - Insurance not expired
     * - Coverage still active
     * - Plafonds not exceeded
     */
    suspend fun validateTiersPayantForCustomer(
        customerId: Long,
        tiersPayantId: Long
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = tiersPayantApi.validateTiersPayantForCustomer(customerId, tiersPayantId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
