package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Store
import retrofit2.Response
import retrofit2.http.GET

/**
 * Store API Service
 * Retrofit interface for store-related API endpoints
 */
interface StoreApiService {

    /**
     * Get current user's store/magasin information
     */
    @GET("api/magasins/current-user-magasin")
    suspend fun getCurrentUserStore(): Response<Store>
}
