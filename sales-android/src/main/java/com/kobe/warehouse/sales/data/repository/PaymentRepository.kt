package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.PaymentApiService
import com.kobe.warehouse.sales.data.model.PaymentMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Payment Repository
 * Handles payment modes and QR codes
 */
class PaymentRepository(
    private val paymentApiService: PaymentApiService
) {

    // Cache payment modes in memory
    private var cachedPaymentModes: List<PaymentMode>? = null

    /**
     * Get all enabled payment modes
     */
    suspend fun getPaymentModes(forceRefresh: Boolean = false): Result<List<PaymentMode>> {
        return withContext(Dispatchers.IO) {
            try {
                // Return cached if available and not forced to refresh
                if (!forceRefresh && cachedPaymentModes != null) {
                    return@withContext Result.success(cachedPaymentModes!!)
                }

                val response = paymentApiService.getPaymentModes()
                if (response.isSuccessful && response.body() != null) {
                    cachedPaymentModes = response.body()!!
                    Result.success(cachedPaymentModes!!)
                } else {
                    Result.failure(Exception("Failed to load payment modes: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }




    /**
     * Get QR code for mobile money payment
     */
    suspend fun getPaymentQrCode(code: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = paymentApiService.getPaymentQrCode(code)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.qrCode)
                } else {
                    Result.failure(Exception("Failed to load QR code: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Clear cached payment modes
     */
    fun clearCache() {
        cachedPaymentModes = null
    }
}
