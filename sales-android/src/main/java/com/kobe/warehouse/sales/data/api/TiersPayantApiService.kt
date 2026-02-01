package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.TiersPayant
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API Service for Tiers Payant (Insurance Provider) operations
 * Backend: TiersPayantResource.java
 */
interface TiersPayantApiService {

    /**
     * Get all tiers payants with filters
     * Backend: GET /tiers-payants?search=...&type=...&groupeTiersPayantId=...
     *
     * @param search Search term (optional)
     * @param type Type filter (optional) - CARNET or ASSURANCE
     * @param groupeTiersPayantId Group filter (optional)
     * @param size Page size (default 20)
     */
    @GET("api/tiers-payants")
    suspend fun searchTiersPayants(
        @Query("search") search: String = "",
        @Query("type") type: String = "",
        @Query("groupeTiersPayantId") groupeTiersPayantId: Int? = null,
        @Query("size") size: Int = 20
    ): Response<List<TiersPayant>>

    /**
     * Create new tiers payant
     * Backend: POST /tiers-payants
     */
    @POST("api/tiers-payants")
    suspend fun createTiersPayant(
        @Body tiersPayant: TiersPayant
    ): Response<com.kobe.warehouse.sales.data.model.TiersPayant>

    /**
     * Update existing tiers payant
     * Backend: PUT /tiers-payants
     * Note: ID is in the body, not in the path
     */
    @PUT("api/tiers-payants")
    suspend fun updateTiersPayant(
        @Body tiersPayant: com.kobe.warehouse.sales.data.model.TiersPayant
    ): Response<com.kobe.warehouse.sales.data.model.TiersPayant>

    /**
     * Delete tiers payant
     * Backend: DELETE /tiers-payants/{id}
     */
    @DELETE("api/tiers-payants/{id}")
    suspend fun deleteTiersPayant(
        @Path("id") id: Int
    ): Response<Void>

    /**
     * Disable tiers payant (soft delete)
     * Backend: DELETE /tiers-payants/desable/{id}
     */
    @DELETE("api/tiers-payants/desable/{id}")
    suspend fun disableTiersPayant(
        @Path("id") id: Int
    ): Response<Void>
}
