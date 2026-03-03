package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.DeconditionApiService
import com.kobe.warehouse.sales.data.model.Decondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decondition Repository
 * Handles deconditioning operations (splitting CH/box into detail units)
 */
class DeconditionRepository(
    private val deconditionApiService: DeconditionApiService
) {

    /**
     * Create a deconditioning operation
     * @param decondition Decondition with qtyMvt and produitId (CH parent ID)
     */
    suspend fun create(decondition: Decondition): Result<Decondition> {
        return withContext(Dispatchers.IO) {
            try {
                val response = deconditionApiService.create(decondition)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Erreur lors du déconditionnement: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
