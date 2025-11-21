package com.kobe.warehouse.sales.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kobe.warehouse.sales.data.api.SalesApiService
import com.kobe.warehouse.sales.data.model.Sale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * API Error Response model
 * Matches backend error format
 */
data class ApiErrorResponse(
    @SerializedName("detail")
    val detail: String? = null,

    @SerializedName("errorKey")
    val errorKey: String? = null,

    @SerializedName("instance")
    val instance: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("status")
    val status: Int? = null,

    @SerializedName("title")
    val title: String? = null
)

/**
 * Sales Repository
 * Handles sales-related business logic and API calls
 *
 * Note: Backend uses single-step finalization.
 * Products and payments are included in the Sale object and sent together.
 */
class SalesRepository(
    private val salesApiService: SalesApiService
) {
    private val gson = Gson()

    /**
     * Get list of  sales
     */
    suspend fun getSales(search: String? = null): Result<List<Sale>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.getSales(search)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get sale by ID
     */
    suspend fun getSaleById(id: Long, date: String): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.getSaleById(id, date)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create and finalize cash sale (single-step)
     * Sale object must include:
     * - salesLines (products)
     * - payments
     * - customer info
     * Backend handles everything in one transaction
     */
    suspend fun createCashSale(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.createCashSale(sale)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    // Parse error body to get meaningful error message
                    val errorMessage = parseErrorResponse(response.errorBody()?.string())
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Parse backend error response
     * Extracts the user-friendly error message from backend error body
     */
    private fun parseErrorResponse(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) {
            return "Erreur inconnue"
        }

        return try {
            val errorResponse = gson.fromJson(errorBody, ApiErrorResponse::class.java)

            // Priority: detail > message > title
            when {
                !errorResponse.detail.isNullOrEmpty() -> errorResponse.detail
                !errorResponse.message.isNullOrEmpty() -> errorResponse.message
                !errorResponse.title.isNullOrEmpty() -> errorResponse.title
                else -> "Erreur lors de la création de la vente"
            }
        } catch (e: Exception) {
            // If parsing fails, return generic error
            "Erreur lors de la création de la vente"
        }
    }
}
