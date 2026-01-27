package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.domain.model.TiersPayant
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Tiers Payant API Service
 * Retrofit interface for tiers payant (insurance provider) endpoints
 */
interface TiersPayantApiService {

    /**
     * Search tiers payants
     * Returns list of insurance providers matching search query
     */
    @GET("api/tiers-payants/search")
    suspend fun searchTiersPayants(
        @Query("q") query: String,
        @Query("size") size: Int = 50
    ): Response<List<TiersPayant>>

    /**
     * Get tiers payant by ID
     */
    @GET("api/tiers-payants/{id}")
    suspend fun getTiersPayantById(
        @Path("id") id: Long
    ): Response<TiersPayant>

    /**
     * Get all active tiers payants
     * Returns only enabled insurance providers
     */
    @GET("api/tiers-payants/actifs")
    suspend fun getActiveTiersPayants(): Response<List<TiersPayant>>

    /**
     * Get tiers payants for a specific customer
     * Returns customer's principal and complementary insurance providers
     */
    @GET("api/customers/{customerId}/tiers-payants")
    suspend fun getCustomerTiersPayants(
        @Path("customerId") customerId: Long
    ): Response<List<TiersPayant>>
}
