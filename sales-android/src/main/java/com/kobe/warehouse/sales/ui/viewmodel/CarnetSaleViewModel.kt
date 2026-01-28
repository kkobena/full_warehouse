package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kobe.warehouse.sales.data.model.Customer

/**
 * CarnetSaleViewModel
 *
 * Manages carnet (credit) sales data for UI display only.
 *
 * IMPORTANT: La validation du crédit et les restrictions sont gérées côté BACKEND.
 *
 * Le backend:
 * - Calcule automatiquement le crédit disponible
 * - Valide les limites de crédit lors de la finalisation
 * - Retourne des erreurs si le crédit est insuffisant
 * - Gère le solde et l'historique du carnet
 *
 * Ce ViewModel se contente de:
 * - Stocker le client sélectionné
 * - Afficher les informations de crédit retournées par le backend
 * - Pas de calculs ni validations locales
 */
class CarnetSaleViewModel : ViewModel() {

    // ===== Customer & Carnet Info =====

    private val _customer = MutableLiveData<Customer?>()
    val customer: LiveData<Customer?> = _customer

    /**
     * Informations de crédit retournées par le backend via API:
     * GET /api/customers/{id}/carnet/balance
     * GET /api/customers/{id}/carnet/limit
     */
    private val _creditLimit = MutableLiveData(0)
    val creditLimit: LiveData<Int> = _creditLimit

    private val _currentBalance = MutableLiveData(0)
    val currentBalance: LiveData<Int> = _currentBalance

    private val _availableCredit = MutableLiveData(0)
    val availableCredit: LiveData<Int> = _availableCredit

    fun setCustomer(customer: Customer) {
        _customer.value = customer
        // TODO: Charger les données carnet depuis le backend via CustomerRepository
        // customerRepository.getCarnetBalance(customer.id)
        // customerRepository.getCarnetLimit(customer.id)
    }

    /**
     * Met à jour les informations de crédit depuis la réponse backend
     */
    fun updateCreditInfo(limit: Int, balance: Int) {
        _creditLimit.value = limit
        _currentBalance.value = balance
        _availableCredit.value = limit - balance
    }

    // ===== Error Handling =====

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }
}
