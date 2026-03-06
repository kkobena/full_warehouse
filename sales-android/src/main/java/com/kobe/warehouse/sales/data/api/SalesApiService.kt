package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleId
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.data.model.SalesStatut
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
   * get liste des vente simplifiées
   * A utiliser pour les ventes simplifiées
   */
    @GET("api/sales/simplified")
    suspend fun getListVenteSimplifiees(
        @Query("search") search: String? = null
    ): Response<List<Sale>>

    /**
     * Get list of preventes  dont le statut est "PENDING"
     * A utiliser pour récupérer les ventes en attente (préventes)
     */
    @GET("api/sales/prevente")
    suspend fun getPreventes(
        @Query("search") search: String? = null,
        @Query("statut") statut: @JvmSuppressWildcards List<SalesStatut>? = listOf(SalesStatut.PROCESSING),
        @Query("userId") userId: Int? = null,

    ): Response<List<Sale>>



  /**
   * Get list of preventes  dont le statut est "ACTIVE"
   * A utiliser pour récupérer les ventes en cours
   */
  @GET("api/sales/prevente")
  suspend fun getVenteEncours(
    @Query("search") search: String? = null,
    @Query("statut") statut: @JvmSuppressWildcards List<SalesStatut>? = listOf(SalesStatut.ACTIVE),
    @Query("userId") userId: Int? = null,

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
     * Delete ongoing sale (prevente)
     * Backend endpoint: DELETE /api/sales/prevente/{id}/{saleDate}
     */
    @DELETE("api/sales/prevente/{id}/{saleDate}")
    suspend fun deleteSale(
        @Path("id") id: Long,
        @Path("saleDate") saleDate: String
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
        @Query("sale_date") saleDate: String
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
     * Backend uses same endpoint as assurance: PUT /api/sales/assurance/add-remise
     */
    @PUT("api/sales/assurance/add-remise")
    suspend fun addDiscountToCarnetSale(
        @Body updateSaleInfo: UpdateSaleInfo
    ): Response<Void>

    /**
     * Set sale line quantity requested (for comptant sales)
     * Backend endpoint: PUT /api/sales/set-item/quantity-requested
     * @param saleLine SaleLine object with updated quantity
     */
    @PUT("api/sales/set-item/quantity-requested")
    suspend fun updateItemQuantityComptant(
        @Body saleLine: SaleLine
    ): Response<SaleLine>

    /**
     * Set sale line quantity requested (for assurance/carnet sales)
     * Backend endpoint: PUT /api/sales/set-item/quantity-requested/assurance
     * @param saleLine SaleLine object with updated quantity
     */
    @PUT("api/sales/set-item/quantity-requested/assurance")
    suspend fun updateItemQuantityAssurance(
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
     * Backend endpoint: POST /api/sales/comptant
     * @param sale Sale object with first sale line
     */
    @POST("api/sales/comptant")
    suspend fun createComptantSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Add product line to existing COMPTANT sale
     * Backend endpoint: POST /api/sales/add-item/comptant
     * @param saleLine Sale line to add
     */
    @POST("api/sales/add-item/comptant")
    suspend fun addItemToComptantSale(
        @Body saleLine: SaleLine
    ): Response<SaleLine>

    /**
     * Create new ASSURANCE/CARNET sale (VO) with first product line
     * Backend endpoint: POST /api/sales/assurance
     * @param sale Sale object with first sale line, customer and tiers payants
     */
    @POST("api/sales/assurance")
    suspend fun createVOSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Add product line to existing ASSURANCE/CARNET sale (VO)
     * Backend endpoint: POST /api/sales/add-item/assurance
     * @param saleLine Sale line to add
     */
    @POST("api/sales/add-item/assurance")
    suspend fun addItemToVOSale(
        @Body saleLine: SaleLine
    ): Response<SaleLine>

    /**
     * Finalize COMPTANT sale with payments
     * Backend endpoint: PUT /api/sales/comptant/save
     * @param sale Sale object with payments list
     * @return Finalized sale with updated amounts
     */
    @PUT("api/sales/comptant/save")
    suspend fun finalizeComptantSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Finalize ASSURANCE/CARNET sale (VO) with payments
     * Backend endpoint: PUT /api/sales/assurance/save
     * @param sale Sale object with payments, tiers payants, numéros de bon
     * @return Finalized sale with partAssure and partTiersPayant calculated
     */
    @PUT("api/sales/assurance/save")
    suspend fun finalizeVOSale(
        @Body sale: Sale
    ): Response<Sale>

    /**
     * Activate/Resume a prevente (change status from PENDING to PROCESSING)
     * Backend endpoint: PUT /api/sales/prevente/activate/{id}/{saleDate}
     * @param id Sale ID
     * @param saleDate Sale date (yyyy-MM-dd)
     */
    @PUT("api/sales/prevente/activate/{id}/{saleDate}")
    suspend fun activatePrevente(
        @Path("id") id: Long,
        @Path("saleDate") saleDate: String
    ): Response<Void>

    /**
     * Transform COMPTANT prevente to vente en cours
     * Backend endpoint: PUT /api/sales/comptant/transform
     * @param saleId SaleId (id + saleDate)
     * @return SaleId of the transformed sale
     */
    @PUT("api/sales/comptant/transform")
    suspend fun transformComptantPrevente(
        @Body saleId: SaleId
    ): Response<SaleId>

    /**
     * Transform ASSURANCE/CARNET prevente to vente en cours
     * Backend endpoint: PUT /api/sales/assurance/transform
     * @param saleId SaleId (id + saleDate)
     * @return SaleId of the transformed sale
     */
    @PUT("api/sales/assurance/transform")
    suspend fun transformAssurancePrevente(
        @Body saleId: SaleId
    ): Response<SaleId>

    /**
     * Add ayant droit (beneficiary) to assurance sale
     * Backend endpoint: PUT /api/sales/assurance/ayant-droit
     * @param updateSaleInfo UpdateSaleInfo with sale ID and ayant droit customer ID as value
     */
    @PUT("api/sales/assurance/ayant-droit")
    suspend fun addAyantDroit(
        @Body updateSaleInfo: UpdateSaleInfo
    ): Response<Void>

    /**
     * Remove discount from assurance/carnet sale
     * Backend endpoint: DELETE /api/sales/assurance/remove-remise/{id}/{saleDate}
     */
    @DELETE("api/sales/assurance/remove-remise/{id}/{saleDate}")
    suspend fun removeDiscountFromAssuranceSale(
        @Path("id") id: Long,
        @Path("saleDate") saleDate: String
    ): Response<Void>
}

