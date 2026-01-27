package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.domain.model.CarnetData
import com.kobe.warehouse.sales.domain.validator.CarnetValidator
import kotlinx.coroutines.launch

/**
 * Carnet Sale ViewModel
 *
 * Manages carnet (credit) sale specific logic:
 * - Credit limit validation
 * - Encours (current balance) tracking
 * - Available credit calculation
 * - Credit usage warnings
 */
class CarnetSaleViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    // ========== Carnet Data ==========

    private val _carnetData = MutableLiveData<CarnetData?>()
    val carnetData: LiveData<CarnetData?> = _carnetData

    private val _canFinalizeSale = MutableLiveData<Boolean>(false)
    val canFinalizeSale: LiveData<Boolean> = _canFinalizeSale

    // ========== Loading & Error ==========

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _warningMessage = MutableLiveData<String?>()
    val warningMessage: LiveData<String?> = _warningMessage

    // Validator
    private val validator = CarnetValidator()

    // ========== Carnet Methods ==========

    /**
     * Load carnet data for customer
     */
    fun loadCarnetData(customer: Customer) {
        viewModelScope.launch {
            _isLoading.value = true
            customerRepository.getCustomerCarnetData(customer.id)
                .fold(
                    onSuccess = { carnetData ->
                        _carnetData.value = carnetData
                        validateCarnetData(carnetData)
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur de chargement des données carnet: ${error.message}"
                        _isLoading.value = false
                    }
                )
        }
    }

    /**
     * Validate carnet data
     */
    private fun validateCarnetData(carnetData: CarnetData) {
        val validation = validator.validateCarnetData(carnetData)
        if (!validation.isValid) {
            _errorMessage.value = validation.getErrorMessage()
            _canFinalizeSale.value = false
            return
        }

        // Check credit usage for warnings
        val usageValidation = validator.validateCreditUsage(carnetData)
        if (usageValidation.hasErrors()) {
            _warningMessage.value = usageValidation.getErrorMessage()
        }

        _canFinalizeSale.value = true
    }

    /**
     * Validate sale amount against credit limit
     */
    fun validateSaleAmount(saleAmount: Int): Boolean {
        val carnetData = _carnetData.value ?: run {
            _errorMessage.value = "Données carnet non disponibles"
            return false
        }

        val validation = validator.validateSaleAmount(carnetData, saleAmount)
        if (!validation.isValid) {
            _errorMessage.value = validation.getErrorMessage()
            _canFinalizeSale.value = false
            return false
        }

        // Show warning if credit usage will be high after sale
        val newEncours = carnetData.calculateNewEncours(saleAmount)
        val newUsage = ((newEncours.toFloat() / carnetData.limiteCredit.toFloat()) * 100).toInt()
        if (newUsage >= 90) {
            _warningMessage.value = "Attention: après cette vente, le client utilisera $newUsage% de sa limite de crédit"
        }

        _canFinalizeSale.value = true
        return true
    }

    /**
     * Get maximum sale amount allowed
     */
    fun getMaxSaleAmount(): Int {
        return validator.getSuggestedMaxAmount(_carnetData.value)
    }

    /**
     * Calculate new credit state after sale
     */
    fun calculateNewCreditState(saleAmount: Int): Triple<Int, Int, Int>? {
        val carnetData = _carnetData.value ?: return null

        val newEncours = carnetData.calculateNewEncours(saleAmount)
        val newCreditDisponible = carnetData.calculateNewCreditDisponible(saleAmount)
        val newUsagePercentage = ((newEncours.toFloat() / carnetData.limiteCredit.toFloat()) * 100).toInt()

        return Triple(newEncours, newCreditDisponible, newUsagePercentage)
    }

    // ========== Utility Methods ==========

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearWarning() {
        _warningMessage.value = null
    }

    fun reset() {
        _carnetData.value = null
        _canFinalizeSale.value = false
        _errorMessage.value = null
        _warningMessage.value = null
    }
}
