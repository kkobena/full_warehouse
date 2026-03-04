package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import kotlinx.coroutines.launch

/**
 * CustomerSelectionViewModel
 *
 * Manages customer search and selection for sales that require a customer
 * (Assurance and Carnet sales)
 */
class CustomerSelectionViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    // ===== Customer Search =====

    private val _customers = MutableLiveData<List<Customer>>()
    val customers: LiveData<List<Customer>> = _customers

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    fun searchCustomers(query: String) {
        if (query.length < 3) {
            _customers.value = emptyList()
            return
        }

        _isSearching.value = true
        viewModelScope.launch {
            customerRepository.searchAssuredCustomers(query).fold(
                onSuccess = { customerList ->
                    _customers.value = customerList
                    _isSearching.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = "Erreur de recherche: ${error.message}"
                    _isSearching.value = false
                }
            )
        }
    }

    fun clearSearch() {
        _customers.value = emptyList()
    }

    // ===== Selected Customer =====

    private val _selectedCustomer = MutableLiveData<Customer?>()
    val selectedCustomer: LiveData<Customer?> = _selectedCustomer

    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
    }

    fun clearSelectedCustomer() {
        _selectedCustomer.value = null
    }

    // ===== Ayants-Droit (Beneficiaries) =====

    private val _ayantsDroit = MutableLiveData<List<Customer>>()
    val ayantsDroit: LiveData<List<Customer>> = _ayantsDroit

    private val _isLoadingAyantsDroit = MutableLiveData(false)
    val isLoadingAyantsDroit: LiveData<Boolean> = _isLoadingAyantsDroit

    fun loadAyantsDroit(customerId: Int) {
        _isLoadingAyantsDroit.value = true
        viewModelScope.launch {
            // TODO: Implement API call to get ayants-droit
            // customerRepository.getAyantsDroit(customerId).fold(
            //     onSuccess = { list ->
            //         _ayantsDroit.value = list
            //         _isLoadingAyantsDroit.value = false
            //     },
            //     onFailure = { error ->
            //         _errorMessage.value = "Erreur de chargement: ${error.message}"
            //         _isLoadingAyantsDroit.value = false
            //     }
            // )

            // For now, return empty list
            _ayantsDroit.value = emptyList()
            _isLoadingAyantsDroit.value = false
        }
    }

    // ===== Customer Validation =====

    /**
     * Check if customer is eligible for insurance sales
     * Requirements:
     * - Must have active insurance
     * - Insurance must not be expired
     */
    fun isEligibleForAssurance(customer: Customer): Boolean {
        // TODO: Add insurance validation logic
        // - Check if customer has active insurance
        // - Check insurance expiration date
        // - Check if insurance is enabled
        return true // Placeholder
    }

    /**
     * Check if customer is eligible for carnet sales
     * Requirements:
     * - Must have carnet account
     * - Carnet must be active
     * - Must have available credit
     */
    fun isEligibleForCarnet(customer: Customer): Boolean {
        // TODO: Add carnet validation logic
        // - Check if customer has carnet
        // - Check if carnet is active
        // - Check credit limit
        return true // Placeholder
    }

    /**
     * Get customer's credit limit for carnet sales
     */
    fun getCustomerCreditLimit(customer: Customer): Int {
        // TODO: Get actual credit limit from customer data
        return 0
    }

    /**
     * Get customer's current carnet balance
     */
    fun getCustomerCarnetBalance(customer: Customer): Int {
        // TODO: Get actual carnet balance from customer data
        return 0
    }

    /**
     * Calculate available credit for customer
     */
    fun getAvailableCredit(customer: Customer): Int {
        val creditLimit = getCustomerCreditLimit(customer)
        val currentBalance = getCustomerCarnetBalance(customer)
        return creditLimit - currentBalance
    }

    // ===== Error Handling =====

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }
}
