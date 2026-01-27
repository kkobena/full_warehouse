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

    /**
     * Get customer insurance data (for Assurance sales)
     * Returns tiers payants, plafond, encours
     */
    @GET("api/customers/{id}/insurance-data")
    suspend fun getCustomerInsuranceData(
        @Path("id") customerId: Long
    ): Response<com.kobe.warehouse.sales.domain.model.InsuranceData>

    /**
     * Get customer carnet data (for Carnet sales)
     * Returns limite credit, encours, credit disponible
     */
    @GET("api/customers/{id}/carnet-data")
    suspend fun getCustomerCarnetData(
        @Path("id") customerId: Long
    ): Response<com.kobe.warehouse.sales.domain.model.CarnetData>

    /**
     * Get ayants-droit (beneficiaries) for a customer
     * Used in Assurance sales to select who the sale is for
     */
    @GET("api/customers/{id}/ayant-droits")
    suspend fun getAyantDroits(
        @Path("id") customerId: Long
    ): Response<List<Customer>>
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
