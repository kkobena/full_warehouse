package com.kobe.warehouse.sales.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kobe.warehouse.sales.data.api.SalesApiService
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.UpdateSaleInfo
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
 * Exception with errorKey from backend API error responses
 * Allows callers to distinguish between different error types (e.g., 'stock', 'stockChInsufisant')
 */
class SalesApiException(
    message: String,
    val errorKey: String? = null
) : Exception(message)

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
     * get liste des vente simplifiées
     * A utiliser pour les ventes simplifiées
     */
    suspend fun getListVenteSimplifiees(search: String? = null): Result<List<Sale>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.getListVenteSimplifiees(search)
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
     * Get list of preventes (ventes mises en attente / depuis le menu prevente)
     */
    suspend fun getPreventes(search: String? = null, userId: Int? = null): Result<List<Sale>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.getPreventes(search, userId = userId)
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
   * Get list  des ventes en cours (menu vente en cours)
   */
  suspend fun getVenteEncours(search: String? = null, userId: Int? = null): Result<List<Sale>> {
    return withContext(Dispatchers.IO) {
      try {
        val response = salesApiService.getVenteEncours(search, userId = userId)
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


                // Log the full JSON being sent
                val saleJson = gson.toJson(sale)
                android.util.Log.d("SalesRepository", "Full Sale JSON: $saleJson")

                val response = salesApiService.createCashSale(sale)
                android.util.Log.d("SalesRepository", "Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {

                    Result.success(response.body()!!)
                } else {
                    // Parse error body to get meaningful error message
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("SalesRepository", "ERROR Response: $errorBody")
                    val errorMessage = parseErrorResponse(errorBody)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                android.util.Log.e("SalesRepository", "EXCEPTION: ${e.message}", e)
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
     * Update sale line quantity requested
     * @param saleLine SaleLine with updated quantity
     * @param natureVente Nature of sale: "COMPTANT", "ASSURANCE", or "CARNET"
     */
    suspend fun updateItemQuantity(
        saleLine: com.kobe.warehouse.sales.data.model.SaleLine,
        natureVente: String = "COMPTANT"
    ): Result<com.kobe.warehouse.sales.data.model.SaleLine> {
        return withContext(Dispatchers.IO) {
            try {

                // Use different endpoint based on sale type
                val response = when (natureVente) {
                    "ASSURANCE", "CARNET" -> salesApiService.updateItemQuantityAssurance(saleLine)
                    else -> salesApiService.updateItemQuantityComptant(saleLine)
                }

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(parseErrorResponseWithKey(errorBody))
                }
            } catch (e: Exception) {
                android.util.Log.e("SalesRepository", "EXCEPTION: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update sale line price
     * @param saleLine SaleLine with updated regularUnitPrice
     */
    suspend fun updateItemPrice(
        saleLine: com.kobe.warehouse.sales.data.model.SaleLine
    ): Result<com.kobe.warehouse.sales.data.model.SaleLine> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.updateItemPrice(saleLine)
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
     * Delete sale line by ID and date
     * @param saleLineId Sale line ID (Long from SaleLineId.id)
     * @param saleDate Sale date (String format: yyyy-MM-dd)
     */
    suspend fun deleteItem(
        saleLineId: Long,
        saleDate: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.deleteItem(saleLineId, saleDate)
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
     * Create new COMPTANT sale with first product line
     * @param sale Sale object with salesLines containing first product
     */
    suspend fun createComptantSale(
        sale: Sale
    ): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {


                val response = salesApiService.createComptantSale(sale)
                android.util.Log.d("SalesRepository", "Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("SalesRepository", "SUCCESS: Comptant sale created")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("SalesRepository", "ERROR: $errorBody")
                    Result.failure(parseErrorResponseWithKey(errorBody))
                }
            } catch (e: Exception) {
                android.util.Log.e("SalesRepository", "EXCEPTION: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Add product line to existing COMPTANT sale
     * @param saleLine SaleLine with saleId, produitId, quantity, price
     */
    suspend fun addItemToComptantSale(
        saleLine: com.kobe.warehouse.sales.data.model.SaleLine
    ): Result<com.kobe.warehouse.sales.data.model.SaleLine> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.addItemToComptantSale(saleLine)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(parseErrorResponseWithKey(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create new ASSURANCE/CARNET sale (VO) with first product line
     * @param sale Sale object with customer, tiers payants, and first product line
     */
    suspend fun createVOSale(
        sale: Sale
    ): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.createVOSale(sale)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(parseErrorResponseWithKey(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Add product line to existing ASSURANCE/CARNET sale (VO)
     * @param saleLine SaleLine with saleId, produitId, quantity, price
     */
    suspend fun addItemToVOSale(
        saleLine: com.kobe.warehouse.sales.data.model.SaleLine
    ): Result<com.kobe.warehouse.sales.data.model.SaleLine> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.addItemToVOSale(saleLine)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(parseErrorResponseWithKey(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Finalize COMPTANT sale with payments
     * Completes cash sale and records payments
     * Backend verifies cash register is open and updates stock
     *
     * @param sale Sale object with payments list populated
     * @return Finalized sale with updated amounts and status
     */
    suspend fun finalizeComptantSale(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {


                // Log the full JSON being sent
                val saleJson = gson.toJson(sale)
                android.util.Log.d("SalesRepository", "Full Sale JSON: $saleJson")

                val response = salesApiService.finalizeComptantSale(sale)
                android.util.Log.d("SalesRepository", "Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("SalesRepository", "SUCCESS: Comptant sale finalized")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("SalesRepository", "ERROR Response: $errorBody")
                    val errorMessage = parseErrorResponse(errorBody)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                android.util.Log.e("SalesRepository", "EXCEPTION: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Finalize ASSURANCE/CARNET sale (VO) with payments
     * Completes insurance/credit sale with tiers payants and payments
     * Backend calculates partAssure, partTiersPayant and verifies plafonds
     *
     * Validations performed by backend:
     * - Customer selected
     * - Tiers payants configured (1-3)
     * - Numéros de bon entered (unless sansBon = true)
     * - Plafond client not exceeded
     *
     * @param sale Sale object with payments, tiersPayants, and numéros de bon
     * @return Finalized sale with partAssure and partTiersPayant calculated
     */
    suspend fun finalizeVOSale(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {


                // Log the full JSON being sent
                val saleJson = gson.toJson(sale)
                android.util.Log.d("SalesRepository", "Full Sale JSON: $saleJson")

                val response = salesApiService.finalizeVOSale(sale)
                android.util.Log.d("SalesRepository", "Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("SalesRepository", "SUCCESS: VO sale finalized")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("SalesRepository", "ERROR Response: $errorBody")
                    val errorMessage = parseErrorResponse(errorBody)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                android.util.Log.e("SalesRepository", "EXCEPTION: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Activate/Resume a prevente (change status from PENDING to PROCESSING)
     * @param saleId Sale ID
     * @param saleDate Sale date (yyyy-MM-dd)
     */
    suspend fun activatePrevente(saleId: Long, saleDate: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.activatePrevente(saleId, saleDate)
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
     * Finalize COMPTANT prevente
     * @param sale Sale object (prevente with statut PROCESSING)
     */
    suspend fun finalizeComptantPrevente(sale: Sale): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.finalizeComptantPrevente(sale)
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
     * Finalize ASSURANCE/CARNET prevente
     * @param sale Sale object (prevente with statut PROCESSING)
     */
    suspend fun finalizeAssurancePrevente(sale: Sale): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.finalizeAssurancePrevente(sale)
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
        return parseErrorResponseWithKey(errorBody).message ?: "Erreur inconnue"
    }

    /**
     * Parse backend error response and return a SalesApiException with errorKey preserved
     * Allows callers to distinguish between different error types (e.g., 'stock', 'stockChInsufisant')
     */
    private fun parseErrorResponseWithKey(errorBody: String?): SalesApiException {
        if (errorBody.isNullOrEmpty()) {
            return SalesApiException("Erreur inconnue")
        }

        return try {
            val errorResponse = gson.fromJson(errorBody, ApiErrorResponse::class.java)

            // Priority: detail > message > title
            val message = when {
                !errorResponse.detail.isNullOrEmpty() -> errorResponse.detail
                !errorResponse.message.isNullOrEmpty() -> errorResponse.message
                !errorResponse.title.isNullOrEmpty() -> errorResponse.title
                else -> "Erreur lors de la création de la vente"
            }

            SalesApiException(message, errorResponse.errorKey)
        } catch (e: Exception) {
            SalesApiException("Erreur lors de la création de la vente")
        }
    }
}
