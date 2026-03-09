package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.ClientTiersPayant
import com.kobe.warehouse.sales.data.model.Customer
import retrofit2.Response
import retrofit2.http.*

/**
 * Customer API Service
 * Retrofit interface for customer-related API endpoints
 */
interface CustomerApiService {

    /**
     * Search uninsured customers only
     * Backend: GET /customers/uninsured?search=...
     */
    @GET("api/customers/uninsured")
    suspend fun searchUninsuredCustomers(
        @Query("search") search: String
    ): Response<List<Customer>>

    /**
     * Search assured customers only
     * Backend: GET /customers/assured?search=...&typeTiersPayant=...
     */
    @GET("api/customers/assured")
    suspend fun searchAssuredCustomers(
        @Query("search") search: String,
        @Query("typeTiersPayant") typeTiersPayant: String? = null,
        @Query("size") size: Int = 5
    ): Response<List<Customer>>

    /**
     * Get customer by ID
     * Backend: GET /customers/{id}
     */
    @GET("api/customers/{id}")
    suspend fun getCustomerById(
        @Path("id") id: Int
    ): Response<Customer>

    /**
     * Create uninsured customer (client comptant)
     * Backend: POST /customers/uninsured
     */
    @POST("api/customers/uninsured")
    suspend fun createUninsuredCustomer(
        @Body customer: CreateCustomerRequest
    ): Response<Customer>

    /**
     * Create insured/assured customer (client assuré)
     * Backend: POST /customers/assured
     */
    @POST("api/customers/assured")
    suspend fun createAssureCustomer(
        @Body customer: Customer
    ): Response<Customer>

    /**
     * Get customer tiers payants
     * Backend: GET /customers/tiers-payants/{id}
     */
    @GET("api/customers/tiers-payants/{id}")
    suspend fun getCustomerTiersPayants(
        @Path("id") customerId: Int
    ): Response<List<ClientTiersPayant>>

    /**
     * Get ayants-droit (beneficiaries) for a customer
     * Backend: GET /customers/ayant-droits/{id}
     */
    @GET("api/customers/ayant-droits/{id}")
    suspend fun getAyantDroits(
        @Path("id") customerId: Int
    ): Response<List<Customer>>

    /**
     * Create ayant-droit (beneficiary) for an assured customer
     * Backend: POST /customers/ayant-droit
     */
    @POST("api/customers/ayant-droit")
    suspend fun createAyantDroit(
        @Body ayantDroit: Customer
    ): Response<Customer>
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
