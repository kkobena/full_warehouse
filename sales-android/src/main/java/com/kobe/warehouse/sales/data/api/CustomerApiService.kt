package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Customer
import retrofit2.Response
import retrofit2.http.*

/**
 * Customer API Service
 * Retrofit interface for customer-related API endpoints
 */
interface CustomerApiService {

    /**
     * Search customers
     */
    @GET("api/customers/lite")
    suspend fun searchCustomers(
        @Query("search") search: String,
        @Query("size") size: Int = 20
    ): Response<List<Customer>>

    /**
     * Get customer by ID
     */
    @GET("api/customers/{id}")
    suspend fun getCustomerById(
        @Path("id") id: Long
    ): Response<Customer>

    /**
     * Create uninsured customer (client comptant)
     */
    @POST("api/customers/uninsured")
    suspend fun createUninsuredCustomer(
        @Body customer: CreateCustomerRequest
    ): Response<Customer>

    /**
     * Get default customer (client comptant)
     */
    @GET("api/customers/default")
    suspend fun getDefaultCustomer(): Response<Customer>
}

/**
 * Create customer request
 */
data class CreateCustomerRequest(
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val email: String? = null
)
