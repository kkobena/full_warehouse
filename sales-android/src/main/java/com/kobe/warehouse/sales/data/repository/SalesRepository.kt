package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.FinalizeSaleRequest
import com.kobe.warehouse.sales.data.api.PaymentRequest
import com.kobe.warehouse.sales.data.api.ReceiptData
import com.kobe.warehouse.sales.data.api.SalesApiService
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sales Repository
 * Handles sales-related business logic and API calls
 */
class SalesRepository(
    private val salesApiService: SalesApiService
) {

    /**
     * Get list of ongoing sales (pré-ventes)
     */
    suspend fun getOngoingSales(search: String? = null, type: String? = null): Result<List<Sale>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.getOngoingSales(search, type)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load sales: ${response.message()}"))
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
                    Result.failure(Exception("Failed to load sale: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create new cash sale
     */
    suspend fun createCashSale(sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.createCashSale(sale)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to create sale: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update existing sale
     */
    suspend fun updateSale(id: Long, date: String, sale: Sale): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.updateSale(id, date, sale)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to update sale: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete ongoing sale
     */
    suspend fun deleteOngoingSale(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.deleteOngoingSale(id)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete sale: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Add product to sale
     */
    suspend fun addSaleLine(id: Long, date: String, saleLine: SaleLine): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.addSaleLine(id, date, saleLine)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to add product: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update sale line quantity
     */
    suspend fun updateSaleLine(id: Long, date: String, lineId: Long, saleLine: SaleLine): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.updateSaleLine(id, date, lineId, saleLine)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to update product: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Remove sale line
     */
    suspend fun removeSaleLine(id: Long, date: String, lineId: Long): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.removeSaleLine(id, date, lineId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to remove product: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Finalize sale with payments
     */
    suspend fun finalizeSale(
        id: Long,
        date: String,
        customerId: Long?,
        payments: List<PaymentRequest>,
        montantVerse: Int,
        montantRendu: Int
    ): Result<Sale> {
        return withContext(Dispatchers.IO) {
            try {
                val request = FinalizeSaleRequest(
                    customerId = customerId,
                    payments = payments,
                    montantVerse = montantVerse,
                    montantRendu = montantRendu,
                    differe = false
                )
                val response = salesApiService.finalizeSale(id, date, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to finalize sale: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get receipt data for printing
     */
    suspend fun getReceipt(id: Long, date: String): Result<ReceiptData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = salesApiService.getReceipt(id, date)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load receipt: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
