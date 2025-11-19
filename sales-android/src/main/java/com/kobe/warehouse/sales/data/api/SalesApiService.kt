package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import retrofit2.Response
import retrofit2.http.*

/**
 * Sales API Service
 * Retrofit interface for sales-related API endpoints
 */
interface SalesApiService {

    /**
     * Get list of ongoing sales (pré-ventes)
     */
    @GET("api/sales/prevente")
    suspend fun getOngoingSales(
        @Query("search") search: String? = null,
        @Query("type") type: String? = null
    ): Response<List<Sale>>

    /**
     * Get sale by ID
     */
    @GET("api/sales/{id}/{date}")
    suspend fun getSaleById(
        @Path("id") id: Long,
        @Path("date") date: String
    ): Response<Sale>

    /**
     * Create new cash sale (comptant)
     */
    @POST("api/sales/comptant")
    suspend fun createCashSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Update existing sale
     */
    @PUT("api/sales/{id}/{date}")
    suspend fun updateSale(
        @Path("id") id: Long,
        @Path("date") date: String,
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Delete ongoing sale (pré-vente)
     */
    @DELETE("api/sales/prevente/{id}")
    suspend fun deleteOngoingSale(
        @Path("id") id: Long
    ): Response<Void>

    /**
     * Add product to sale
     */
    @POST("api/sales/{id}/{date}/line")
    suspend fun addSaleLine(
        @Path("id") id: Long,
        @Path("date") date: String,
        @Body saleLine: SaleLine
    ): Response<Sale>

    /**
     * Update sale line quantity
     */
    @PUT("api/sales/{id}/{date}/line/{lineId}")
    suspend fun updateSaleLine(
        @Path("id") id: Long,
        @Path("date") date: String,
        @Path("lineId") lineId: Long,
        @Body saleLine: SaleLine
    ): Response<Sale>

    /**
     * Remove sale line
     */
    @DELETE("api/sales/{id}/{date}/line/{lineId}")
    suspend fun removeSaleLine(
        @Path("id") id: Long,
        @Path("date") date: String,
        @Path("lineId") lineId: Long
    ): Response<Sale>

    /**
     * Finalize sale (checkout/payment)
     */
    @POST("api/sales/{id}/{date}/finalize")
    suspend fun finalizeSale(
        @Path("id") id: Long,
        @Path("date") date: String,
        @Body finalizeRequest: FinalizeSaleRequest
    ): Response<Sale>

    /**
     * Print receipt
     */
    @GET("api/sales/{id}/{date}/receipt")
    suspend fun getReceipt(
        @Path("id") id: Long,
        @Path("date") date: String
    ): Response<ReceiptData>

    /**
     * Get completed sales (journal)
     */
    @GET("api/sales")
    suspend fun getCompletedSales(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("search") search: String? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null
    ): Response<SalesPageResponse>
}

/**
 * Finalize sale request
 */
data class FinalizeSaleRequest(
    val customerId: Long? = null,
    val payments: List<PaymentRequest>,
    val montantVerse: Int = 0,
    val montantRendu: Int = 0,
    val differe: Boolean = false
)

/**
 * Payment request
 */
data class PaymentRequest(
    val paymentModeCode: String,
    val amount: Int
)

/**
 * Receipt data
 */
data class ReceiptData(
    val sale: Sale,
    val storeName: String,
    val storeAddress: String,
    val storePhone: String,
    val receiptNumber: String,
    val printDate: String
)

/**
 * Sales page response for pagination
 */
data class SalesPageResponse(
    val content: List<Sale>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
)
