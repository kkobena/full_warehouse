package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Remise
import retrofit2.Response
import retrofit2.http.GET

/**
 * Remise API Service
 * Retrofit interface for remise (discount) endpoints
 */
interface RemiseApiService {

    /**
     * Get all remises
     * Backend: GET /api/remises
     */
    @GET("api/remises")
    suspend fun getRemises(): Response<List<Remise>>
}
