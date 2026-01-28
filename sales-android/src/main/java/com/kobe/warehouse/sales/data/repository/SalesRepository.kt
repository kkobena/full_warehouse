package com.kobe.warehouse.sales.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kobe.warehouse.sales.data.api.SalesApiService
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.domain.model.UpdateSaleInfo
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
     * Get list of ongoing sales (ventes en cours)
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
     * Get list of preventes (ventes mises en attente / on hold)
     */
    suspend fun getPreventes(search: String? = null): Result<List<Sale>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.getPreventes(search)
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
     * Delete ongoing sale
     */
    suspend fun deleteSale(id: Long, date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.deleteSale(id, date)
                if (response.isSuccessful) {
                    Result.success(Unit)
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
     * Put cash sale on hold (save as prevente)
     * Saves current sale state without finalizing payment
     */
    suspend fun putCashSaleOnHold(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.putCashSaleOnHold(sale)
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
     * Finalize carnet sale (credit sale)
     * Sale is added to customer's carnet (credit account)
     */
    suspend fun finalizeCarnetSale(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.finalizeCarnetSale(sale)
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
     * Get carnet purchase history for a customer
     */
    suspend fun getCarnetHistory(customerId: Long): Result<List<Sale>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.getCarnetHistory(customerId)
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
     * Create assurance sale (insurance sale)
     * Creates a new sale with tiers payants (insurance providers)
     */
    suspend fun createAssuranceSale(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.createAssuranceSale(sale)
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
     * Put assurance sale on hold (save as prevente)
     * Saves current insurance sale state without finalizing
     */
    suspend fun putAssuranceSaleOnHold(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.putAssuranceSaleOnHold(sale)
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
     * Finalize assurance sale (insurance sale)
     * Completes sale with insurance data (tiers payants, prescription, etc.)
     * Backend calculates partAssure, partTiersPayant, costAmount
     */
    suspend fun finalizeAssuranceSale(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.finalizeAssuranceSale(sale)
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
     * Transform sale between types (COMPTANT, ASSURANCE, CARNET)
     * Converts an existing sale from one nature to another
     *
     * @param natureVente Target sale type: COMPTANT, ASSURANCE, or CARNET
     * @param saleId Sale ID
     * @param saleDate Sale date (format: yyyy-MM-dd)
     * @return Result with new SaleId after transformation
     */
    suspend fun transformSale(
        natureVente: String,
        saleId: Long,
        saleDate: String
    ): Result<com.kobe.warehouse.sales.data.model.SaleId> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.transformSale(natureVente, saleId, saleDate)
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
     * Add discount to sale
     * Applies discount based on sale type (comptant, assurance, or carnet)
     */
    suspend fun addDiscountToSale(
        updateSaleInfo: UpdateSaleInfo,
        saleType: String = "COMPTANT"
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = when (saleType) {
                    "ASSURANCE" -> salesApiService.addDiscountToAssuranceSale(updateSaleInfo)
                    "CARNET" -> salesApiService.addDiscountToCarnetSale(updateSaleInfo)
                    else -> salesApiService.addDiscountToCashSale(updateSaleInfo)
                }

                if (response.isSuccessful) {
                    Result.success(Unit)
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
     * Remove discount from cash sale
     */
    suspend fun removeDiscountFromSale(id: Long, saleDate: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.removeDiscountFromCashSale(id, saleDate)
                if (response.isSuccessful) {
                    Result.success(Unit)
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
