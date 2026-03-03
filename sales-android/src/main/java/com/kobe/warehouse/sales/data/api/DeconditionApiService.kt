package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Decondition
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Decondition API Service
 * Retrofit interface for deconditioning operations
 */
interface DeconditionApiService {

    @POST("api/deconditions")
    suspend fun create(@Body decondition: Decondition): Response<Void>
}
