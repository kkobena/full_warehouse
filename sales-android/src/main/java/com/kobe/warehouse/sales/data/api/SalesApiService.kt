package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import retrofit2.Response
import retrofit2.http.*

/**
 * Sales API Service
 * Retrofit interface for sales-related API endpoints
 *
 * Note: Backend uses single-step finalization.
 * All sale data (products, payments) is sent in one createCashSale call.
 */
interface SalesApiService {

    /**
     * Get list of ongoing sales (ventes)
     */
    @GET("api/sales/simplified")
    suspend fun getSales(
        @Query("search") search: String? = null
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
     * Create and finalize cash sale (single-step)
     * Sends complete sale object with products (salesLines) and payments
     * Backend handles everything in one transaction
     */
    @POST("api/sales/simplified/save")
    suspend fun createCashSale(
        @Body sale: Sale
    ): Response<Sale>
}

