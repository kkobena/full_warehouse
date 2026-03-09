package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.TiersPayantApiService
import com.kobe.warehouse.sales.data.model.TiersPayant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for Tiers Payant (Insurance Provider) operations
 * Backend: TiersPayantResource.java
 */
class TiersPayantRepository(
    private val tiersPayantApi: TiersPayantApiService
) {

    /**
     * Search tiers payants with filters
     * Backend: GET /tiers-payants?search=...&type=...
     *
     * @param search Search term (name or code)
     * @param type Type filter: "CARNET" or "ASSURANCE" (empty = all types)
     * @param groupeTiersPayantId Optional group filter
     * @param size Page size
     */
    suspend fun searchTiersPayants(
        search: String = "",
        type: String = "",// "CARNET", "ASSURANCE", Ou vide pour les deux pour ajout de tiers payant complementaire
        groupeTiersPayantId: Int? = null,
        size: Int = 5
    ): Result<List<TiersPayant>> = withContext(Dispatchers.IO) {
        try {
            val response = tiersPayantApi.searchTiersPayants(search, type, groupeTiersPayantId, size)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur recherche tiers payants: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all active tiers payants (no filter)
     * Backend: GET /tiers-payants
     */
    suspend fun getAllTiersPayants(): Result<List<TiersPayant>> =
        searchTiersPayants(search = "", type = "")



    /**
     * Create new tiers payant
     * Backend: POST /tiers-payants
     */
    suspend fun createTiersPayant(
        tiersPayant: TiersPayant
    ): Result<TiersPayant> = withContext(Dispatchers.IO) {
        try {
            val response = tiersPayantApi.createTiersPayant(tiersPayant)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur lors de la création: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update existing tiers payant
     * Backend: PUT /tiers-payants
     * Note: ID is in the body, not passed separately
     */
    suspend fun updateTiersPayant(
        tiersPayant: TiersPayant
    ): Result<TiersPayant> = withContext(Dispatchers.IO) {
        try {
            val response = tiersPayantApi.updateTiersPayant(tiersPayant)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur lors de la mise à jour: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
