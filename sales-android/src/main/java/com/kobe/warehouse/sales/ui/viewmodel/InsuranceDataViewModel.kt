package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.model.TiersPayant
import kotlinx.coroutines.launch

/**
 * InsuranceDataViewModel
 *
 * Manages insurance-specific data for Assurance sales:
 * - Tiers payants selection
 * - Prescription type and number
 * - Coverage calculations
 */
class InsuranceDataViewModel : ViewModel() {

    // ===== Tiers Payants =====

    private val _selectedTiersPayants = MutableLiveData<List<TiersPayant>>(emptyList())
    val selectedTiersPayants: LiveData<List<TiersPayant>> = _selectedTiersPayants

    fun addTiersPayant(tiersPayant: TiersPayant) {
        val current = _selectedTiersPayants.value ?: emptyList()

        // Simply add the tiers payant
        _selectedTiersPayants.value = current + tiersPayant
    }

    fun removeTiersPayant(tiersPayant: TiersPayant) {
        val current = _selectedTiersPayants.value ?: emptyList()
        _selectedTiersPayants.value = current.filter { it.id != tiersPayant.id }
    }

    fun clearTiersPayants() {
        _selectedTiersPayants.value = emptyList()
    }

    // ===== Prescription Data =====

    private val _prescriptionType = MutableLiveData<String>("PRESCRIPTION")
    val prescriptionType: LiveData<String> = _prescriptionType

    private val _prescriptionNumber = MutableLiveData<String>()
    val prescriptionNumber: LiveData<String> = _prescriptionNumber

    fun setPrescriptionType(type: String) {
        _prescriptionType.value = type
    }

    fun setPrescriptionNumber(number: String) {
        _prescriptionNumber.value = number
    }

    // ===== Coverage Calculations =====

    /**
     * NOTE: Part assurance et part client sont calculées côté BACKEND
     *
     * Le backend retourne dans la réponse Sale:
     * - salesAmount (montant total)
     * - partAssure (part assurance calculée par le backend)
     * - partTiersPayant (part tiers payant calculée par le backend)
     * - costAmount (part client = montant à payer par le client)
     *
     * Ces calculs sont centralisés côté backend pour garantir l'harmonisation
     * entre l'application web et l'application mobile.
     *
     * Le ViewModel se contente de stocker les tiers payants sélectionnés
     * et les envoie au backend lors de la finalisation de la vente.
     */

    // ===== Validation =====

    fun validateInsuranceData(): Boolean {
        // Check tiers payants
        val tiers = _selectedTiersPayants.value ?: emptyList()
        if (tiers.isEmpty()) {
            _errorMessage.value = "Au moins un tiers payant est requis"
            return false
        }

        return true
    }

    // ===== Error Handling =====

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }
}
