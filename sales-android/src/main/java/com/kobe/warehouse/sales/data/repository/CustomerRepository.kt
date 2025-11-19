package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.CreateCustomerRequest
import com.kobe.warehouse.sales.data.api.CustomerApiService
import com.kobe.warehouse.sales.data.model.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Customer Repository
 * Handles customer search and creation
 */
class CustomerRepository(
    private val customerApiService: CustomerApiService
) {

    // Cache default customer
    private var cachedDefaultCustomer: Customer? = null

    /**
     * Search customers
     */
    suspend fun searchCustomers(search: String, size: Int = 20): Result<List<Customer>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = customerApiService.searchCustomers(search, size)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to search customers: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get customer by ID
     */
    suspend fun getCustomerById(id: Long): Result<Customer> {
        return withContext(Dispatchers.IO) {
            try {
                val response = customerApiService.getCustomerById(id)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Customer not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create uninsured customer (client comptant)
     */
    suspend fun createUninsuredCustomer(
        firstName: String,
        lastName: String,
        phone: String? = null,
        email: String? = null
    ): Result<Customer> {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreateCustomerRequest(
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    email = email
                )
                val response = customerApiService.createUninsuredCustomer(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to create customer: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get default customer (client comptant)
     */
    suspend fun getDefaultCustomer(forceRefresh: Boolean = false): Result<Customer> {
        return withContext(Dispatchers.IO) {
            try {
                // Return cached if available
                if (!forceRefresh && cachedDefaultCustomer != null) {
                    return@withContext Result.success(cachedDefaultCustomer!!)
                }

                val response = customerApiService.getDefaultCustomer()
                if (response.isSuccessful && response.body() != null) {
                    cachedDefaultCustomer = response.body()!!
                    Result.success(cachedDefaultCustomer!!)
                } else {
                    Result.failure(Exception("Failed to load default customer: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Clear cached data
     */
    fun clearCache() {
        cachedDefaultCustomer = null
    }
}
