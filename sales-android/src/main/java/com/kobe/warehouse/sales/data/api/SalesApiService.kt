package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleId
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.data.model.UpdateSaleInfo
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

    /**
     * Update sale line quantity sold
     * Backend endpoint: PUT /api/sales/update-item/quantity-sold
     * @param saleLine SaleLine object with updated quantity
     */
    @PUT("api/sales/update-item/quantity-sold")
    suspend fun updateItemQuantity(
        @Body saleLine: SaleLine
    ): Response<SaleLine>

    /**
     * Update sale line price (requires authorization)
     * Backend endpoint: PUT /api/sales/update-item/price
     * @param saleLine SaleLine object with updated price
     */
    @PUT("api/sales/update-item/price")
    suspend fun updateItemPrice(
        @Body saleLine: SaleLine
    ): Response<SaleLine>

    /**
     * Delete sale line by ID and date (requires authorization)
     * Backend endpoint: DELETE /api/sales/delete-item/{id}/{saleDate}
     * @param id Sale line ID
     * @param saleDate Sale date (yyyy-MM-dd)
     */
    @DELETE("api/sales/delete-item/{id}/{saleDate}")
    suspend fun deleteItem(
        @Path("id") id: Long,
        @Path("saleDate") saleDate: String
    ): Response<Void>

    /**
     * Create new COMPTANT sale with first product line
     * Backend endpoint: POST /api/sales/comptant/create
     * @param sale Sale object with first sale line
     */
    @POST("api/sales/comptant/create")
    suspend fun createComptantSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Add product line to existing COMPTANT sale
     * Backend endpoint: POST /api/sales/comptant/add-item
     * @param saleLine Sale line to add
     */
    @POST("api/sales/comptant/add-item")
    suspend fun addItemToComptantSale(
        @Body saleLine: SaleLine
    ): Response<SaleLine>

    /**
     * Create new ASSURANCE/CARNET sale (VO) with first product line
     * Backend endpoint: POST /api/sales/vo/create
     * @param sale Sale object with first sale line, customer and tiers payants
     */
    @POST("api/sales/vo/create")
    suspend fun createVOSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Add product line to existing ASSURANCE/CARNET sale (VO)
     * Backend endpoint: POST /api/sales/vo/add-item
     * @param saleLine Sale line to add
     */
    @POST("api/sales/vo/add-item")
    suspend fun addItemToVOSale(
        @Body saleLine: SaleLine
    ): Response<SaleLine>

    /**
     * Finalize COMPTANT sale with payments
     * Backend endpoint: POST /api/sales/comptant/finalize
     * @param sale Sale object with payments list
     * @return Finalized sale with updated amounts
     */
    @POST("api/sales/comptant/finalize")
    suspend fun finalizeComptantSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Finalize ASSURANCE/CARNET sale (VO) with payments
     * Backend endpoint: POST /api/sales/vo/finalize
     * @param sale Sale object with payments, tiers payants, numéros de bon
     * @return Finalized sale with partAssure and partTiersPayant calculated
     */
    @POST("api/sales/vo/finalize")
    suspend fun finalizeVOSale(
        @Body sale: Sale
    ): Response<Sale>
}

