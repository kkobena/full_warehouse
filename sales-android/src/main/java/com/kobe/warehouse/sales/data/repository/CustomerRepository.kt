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

    /**
     * Search uninsured customers only
     * Backend: GET /customers/uninsured?search=...
     */
    suspend fun searchUninsuredCustomers(search: String): Result<List<Customer>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = customerApiService.searchUninsuredCustomers(search)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to search uninsured customers: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Search assured customers only
     * Backend: GET /customers/assured?search=...
     */
    suspend fun searchAssuredCustomers(search: String, typeTiersPayant: String? = null, size: Int = 5): Result<List<Customer>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = customerApiService.searchAssuredCustomers(search, typeTiersPayant, size)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to search assured customers: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get customer by ID
     * Backend: GET /customers/{id}
     */
    suspend fun getCustomerById(id: Int): Result<Customer> {
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
     * Create uninsured customer (client comptant) - overload accepting Customer object
     */
    suspend fun createUninsuredCustomer(customer: Customer): Result<Customer> {
        return createUninsuredCustomer(
            firstName = customer.firstName,
            lastName = customer.lastName,
            phone = customer.phone,
            email = customer.email
        )
    }

    /**
     * Create insured customer (client assuré)
     */
    suspend fun createAssureCustomer(customer: Customer): Result<Customer> {
        return withContext(Dispatchers.IO) {
            try {
                val response = customerApiService.createAssureCustomer(customer)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to create assure customer: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get customer tiers payants
     * Backend: GET /customers/tiers-payants/{id}
     */
    suspend fun getCustomerTiersPayants(customerId: Int): Result<List<com.kobe.warehouse.sales.data.model.ClientTiersPayant>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = customerApiService.getCustomerTiersPayants(customerId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load tiers payants: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get ayants-droit (beneficiaries) for a customer
     * Backend: GET /customers/ayant-droits/{id}
     */
    suspend fun getAyantDroits(customerId: Int): Result<List<Customer>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = customerApiService.getAyantDroits(customerId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load ayants-droit: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create Carnet customer with tiers payant
     *
     * @param firstName Customer first name
     * @param lastName Customer last name
     * @param phone Customer phone (optional)
     * @param dateNaiss Date of birth (optional)
     * @param tiersPayantId Tiers payant ID
     * @param num Matricule number
     * @param taux Coverage rate (percentage)
     * @return Result with created Customer
     */
    suspend fun createCarnetCustomer(
        firstName: String,
        lastName: String,
        phone: String?,
        dateNaiss: String?,
        tiersPayantId: Long,
        num: String,
        taux: Int
    ): Result<Customer> {
        return withContext(Dispatchers.IO) {
            try {
                // Build Customer object with tiers payant information
                val customer = Customer(
                    id = 0, // Will be assigned by backend
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    // Note: Backend API should handle the tiers payant association
                    // This might need adjustment based on actual backend API structure
                    tiersPayants = listOf(
                        com.kobe.warehouse.sales.data.model.ClientTiersPayant(
                            customerId = 0,
                            tiersPayantId = tiersPayantId,
                            tiersPayantName = "", // Will be filled by backend
                            num = num,
                            taux = taux,
                            priorite = com.kobe.warehouse.sales.data.model.PrioriteTiersPayant.R0,
                            typeTiersPayant = "PRINCIPAL"
                        )
                    )
                )

                val response = customerApiService.createAssureCustomer(customer)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Erreur création client Carnet: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
