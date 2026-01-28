package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleId
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.domain.model.UpdateSaleInfo
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
     * Get list of ongoing sales (ventes en cours)
     */
    @GET("api/sales/simplified")
    suspend fun getSales(
        @Query("search") search: String? = null
    ): Response<List<Sale>>

    /**
     * Get list of preventes (ventes mises en attente)
     */
    @GET("api/sales/prevente")
    suspend fun getPreventes(
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

    /**
     * Delete ongoing sale
     */
    @DELETE("api/sales/ongoing/{id}/{date}")
    suspend fun deleteSale(
        @Path("id") id: Long,
        @Path("date") date: String
    ): Response<Unit>

    /**
     * Put cash sale on hold (save as prevente)
     * Saves current sale state without finalizing
     */
    @PUT("api/sales/comptant/put-on-hold")
    suspend fun putCashSaleOnHold(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Finalize carnet sale (credit sale)
     * Sale is added to customer's carnet account
     */
    @POST("api/sales/carnet/save")
    suspend fun finalizeCarnetSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Get carnet purchase history for a customer
     */
    @GET("api/sales/carnet/history/{customerId}")
    suspend fun getCarnetHistory(
        @Path("customerId") customerId: Long
    ): Response<List<Sale>>

    /**
     * Create assurance sale
     * Creates a new insurance sale with tiers payants
     */
    @POST("api/sales/assurance")
    suspend fun createAssuranceSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Put assurance sale on hold
     * Saves insurance sale as prevente without finalizing
     */
    @PUT("api/sales/assurance/put-on-hold")
    suspend fun putAssuranceSaleOnHold(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Finalize assurance sale (insurance sale)
     * Sale with insurance data (tiers payants, prescription, etc.)
     * Backend calculates partAssure, partTiersPayant, costAmount
     */
    @PUT("api/sales/assurance/save")
    suspend fun finalizeAssuranceSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Transform sale between types (COMPTANT, ASSURANCE, CARNET)
     * Converts an existing sale from one nature to another
     * Backend endpoint: GET /api/sales/assurance/transform
     */
    @GET("api/sales/assurance/transform")
    suspend fun transformSale(
        @Query("natureVente") natureVente: String,
        @Query("saleId") saleId: Long,
        @Query("saleDate") saleDate: String
    ): Response<SaleId>

    /**
     * Add discount to cash sale
     * Applies discount to the entire sale
     */
    @PUT("api/sales/comptant/add-remise")
    suspend fun addDiscountToCashSale(
        @Body updateSaleInfo: UpdateSaleInfo
    ): Response<Void>

    /**
     * Remove discount from cash sale
     */
    @DELETE("api/sales/comptant/remove-remise/{id}/{saleDate}")
    suspend fun removeDiscountFromCashSale(
        @Path("id") id: Long,
        @Path("saleDate") saleDate: String
    ): Response<Void>

    /**
     * Add discount to assurance sale
     * Applies discount to the entire sale
     */
    @PUT("api/sales/assurance/add-remise")
    suspend fun addDiscountToAssuranceSale(
        @Body updateSaleInfo: UpdateSaleInfo
    ): Response<Void>

    /**
     * Add discount to carnet sale
     * Applies discount to the entire sale
     */
    @PUT("api/sales/carnet/add-remise")
    suspend fun addDiscountToCarnetSale(
        @Body updateSaleInfo: UpdateSaleInfo
    ): Response<Void>
}

