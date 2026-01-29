package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.domain.model.TiersPayant
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API Service for Tiers Payant (Insurance Provider) operations
 */
interface TiersPayantApiService {

    /**
     * Get all active tiers payants
     */
    @GET("api/tiers-payants")
    suspend fun getAllTiersPayants(): Response<List<TiersPayant>>

    /**
     * Search tiers payants by name or code
     */
    @GET("api/tiers-payants")
    suspend fun searchTiersPayants(
        @Query("search") query: String
    ): Response<List<TiersPayant>>

    /**
     * Get tiers payant by ID
     */
    @GET("api/tiers-payants/{id}")
    suspend fun getTiersPayantById(
        @Path("id") id: Long
    ): Response<TiersPayant>

    /**
     * Get tiers payants for a specific customer
     */
    @GET("api/tiers-payants/customer/{customerId}")
    suspend fun getTiersPayantsForCustomer(
        @Path("customerId") customerId: Long
    ): Response<List<TiersPayant>>

    /**
     * Validate if a tiers payant is valid for a customer
     */
    @GET("api/tiers-payants/validate")
    suspend fun validateTiersPayantForCustomer(
        @Query("customerId") customerId: Long,
        @Query("tiersPayantId") tiersPayantId: Long
    ): Response<Boolean>
}
