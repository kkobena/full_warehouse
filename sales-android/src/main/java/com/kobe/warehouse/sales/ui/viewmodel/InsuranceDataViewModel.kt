package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.TiersPayantRepository
import com.kobe.warehouse.sales.domain.model.InsuranceData
import com.kobe.warehouse.sales.domain.model.PrescriptionType
import com.kobe.warehouse.sales.domain.model.TiersPayant
import com.kobe.warehouse.sales.domain.validator.AssuranceValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Insurance Data ViewModel
 *
 * Manages insurance-related data for Assurance sales:
 * - Tiers payant selection (principal + complementaires)
 * - Prescription type and numero bon
 * - Coverage rates
 * - Ayant droit selection
 * - Part client/assurance calculation
 */
class InsuranceDataViewModel(
    private val tiersPayantRepository: TiersPayantRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    // ========== Tiers Payant Principal ==========

    private val _tiersPayantPrincipal = MutableLiveData<TiersPayant?>()
    val tiersPayantPrincipal: LiveData<TiersPayant?> = _tiersPayantPrincipal

    private val _tiersPayantSearchResults = MutableLiveData<List<TiersPayant>>()
    val tiersPayantSearchResults: LiveData<List<TiersPayant>> = _tiersPayantSearchResults

    private var tiersPayantSearchJob: Job? = null

    // ========== Tiers Payants Complementaires ==========

    private val _tiersPayantsComplementaires = MutableLiveData<List<TiersPayant>>(emptyList())
    val tiersPayantsComplementaires: LiveData<List<TiersPayant>> = _tiersPayantsComplementaires

    // Map of tiers payant ID to coverage rate for complementaires
    private val complementaireCoverageRates = mutableMapOf<Long, Int>()

    // ========== Prescription ==========

    private val _prescriptionType = MutableLiveData<PrescriptionType>(PrescriptionType.ORDONNANCE)
    val prescriptionType: LiveData<PrescriptionType> = _prescriptionType

    private val _numeroBon = MutableLiveData<String?>()
    val numeroBon: LiveData<String?> = _numeroBon

    private val _isNumeroBonRequired = MutableLiveData<Boolean>(false)
    val isNumeroBonRequired: LiveData<Boolean> = _isNumeroBonRequired

    // ========== Coverage Rates ==========

    private val _tauxCouverturePrincipal = MutableLiveData<Int>(0)
    val tauxCouverturePrincipal: LiveData<Int> = _tauxCouverturePrincipal

    private val _totalCoverageRate = MutableLiveData<Int>(0)
    val totalCoverageRate: LiveData<Int> = _totalCoverageRate

    private val _clientShareRate = MutableLiveData<Int>(100)
    val clientShareRate: LiveData<Int> = _clientShareRate

    // ========== Ayant Droit ==========

    private val _ayantDroit = MutableLiveData<Customer?>()
    val ayantDroit: LiveData<Customer?> = _ayantDroit

    private val _ayantDroitsList = MutableLiveData<List<Customer>>()
    val ayantDroitsList: LiveData<List<Customer>> = _ayantDroitsList

    // ========== Loading & Error ==========

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Validator
    private val validator = AssuranceValidator()

    // ========== Tiers Payant Methods ==========

    /**
     * Search tiers payants with debounce
     */
    fun searchTiersPayants(query: String) {
        tiersPayantSearchJob?.cancel()
        tiersPayantSearchJob = viewModelScope.launch {
            delay(300) // Debounce 300ms

            if (query.length < 2) {
                _tiersPayantSearchResults.value = emptyList()
                return@launch
            }

            _isLoading.value = true
            tiersPayantRepository.searchTiersPayants(query)
                .fold(
                    onSuccess = { tiersPayants ->
                        _tiersPayantSearchResults.value = tiersPayants
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur de recherche: ${error.message}"
                        _tiersPayantSearchResults.value = emptyList()
                        _isLoading.value = false
                    }
                )
        }
    }

    /**
     * Select tiers payant principal
     */
    fun selectTiersPayantPrincipal(tiersPayant: TiersPayant) {
        _tiersPayantPrincipal.value = tiersPayant
        _tauxCouverturePrincipal.value = tiersPayant.tauxCouverture
        _tiersPayantSearchResults.value = emptyList()
        recalculateCoverageRates()
    }

    /**
     * Clear tiers payant principal
     */
    fun clearTiersPayantPrincipal() {
        _tiersPayantPrincipal.value = null
        _tauxCouverturePrincipal.value = 0
        recalculateCoverageRates()
    }

    /**
     * Update taux couverture principal
     */
    fun updateTauxCouverturePrincipal(newTaux: Int) {
        if (newTaux in 0..100) {
            _tauxCouverturePrincipal.value = newTaux
            recalculateCoverageRates()
        }
    }

    /**
     * Add tiers payant complementaire
     */
    fun addTiersPayantComplementaire(tiersPayant: TiersPayant) {
        val current = _tiersPayantsComplementaires.value ?: emptyList()

        // Check if already added
        if (current.any { it.id == tiersPayant.id }) {
            _errorMessage.value = "Ce tiers payant est déjà ajouté"
            return
        }

        // Check if same as principal
        if (_tiersPayantPrincipal.value?.id == tiersPayant.id) {
            _errorMessage.value = "Ce tiers payant est déjà défini comme principal"
            return
        }

        val updated = current + tiersPayant
        _tiersPayantsComplementaires.value = updated
        complementaireCoverageRates[tiersPayant.id] = tiersPayant.tauxCouverture
        recalculateCoverageRates()
    }

    /**
     * Remove tiers payant complementaire
     */
    fun removeTiersPayantComplementaire(tiersPayant: TiersPayant) {
        val current = _tiersPayantsComplementaires.value ?: emptyList()
        _tiersPayantsComplementaires.value = current.filter { it.id != tiersPayant.id }
        complementaireCoverageRates.remove(tiersPayant.id)
        recalculateCoverageRates()
    }

    /**
     * Update taux couverture complementaire
     */
    fun updateTauxComplementaire(tiersPayant: TiersPayant, newTaux: Int) {
        if (newTaux in 0..100) {
            complementaireCoverageRates[tiersPayant.id] = newTaux
            recalculateCoverageRates()
        }
    }

    /**
     * Recalculate total coverage rates
     */
    private fun recalculateCoverageRates() {
        val principalTaux = _tauxCouverturePrincipal.value ?: 0
        val complementaireTaux = complementaireCoverageRates.values.sum()
        val total = (principalTaux + complementaireTaux).coerceAtMost(100)

        _totalCoverageRate.value = total
        _clientShareRate.value = 100 - total

        // Validate total doesn't exceed 100%
        if (principalTaux + complementaireTaux > 100) {
            _errorMessage.value = "Le taux de couverture total ne peut pas dépasser 100%"
        }
    }

    // ========== Prescription Methods ==========

    /**
     * Set prescription type
     */
    fun setPrescriptionType(type: PrescriptionType) {
        _prescriptionType.value = type
        _isNumeroBonRequired.value = type.requiresNumeroBon()

        // Clear numero bon if not required
        if (!type.requiresNumeroBon()) {
            _numeroBon.value = null
        }
    }

    /**
     * Set numero bon
     */
    fun setNumeroBon(numero: String?) {
        _numeroBon.value = numero
    }

    // ========== Ayant Droit Methods ==========

    /**
     * Load ayants-droit for customer
     */
    fun loadAyantDroits(customerId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            customerRepository.getAyantDroits(customerId)
                .fold(
                    onSuccess = { ayantDroits ->
                        _ayantDroitsList.value = ayantDroits
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur de chargement des ayants-droit: ${error.message}"
                        _ayantDroitsList.value = emptyList()
                        _isLoading.value = false
                    }
                )
        }
    }

    /**
     * Select ayant droit
     */
    fun selectAyantDroit(ayantDroit: Customer) {
        _ayantDroit.value = ayantDroit
    }

    /**
     * Clear ayant droit
     */
    fun clearAyantDroit() {
        _ayantDroit.value = null
    }

    // ========== Build InsuranceData ==========

    /**
     * Build complete InsuranceData object
     */
    fun buildInsuranceData(customer: Customer): InsuranceData? {
        val tiersPayantPrincipal = _tiersPayantPrincipal.value ?: return null
        val prescriptionType = _prescriptionType.value ?: PrescriptionType.ORDONNANCE
        val tauxPrincipal = _tauxCouverturePrincipal.value ?: 0

        return InsuranceData(
            tiersPayantPrincipal = tiersPayantPrincipal,
            tiersPayantsComplementaires = _tiersPayantsComplementaires.value ?: emptyList(),
            prescriptionType = prescriptionType,
            numeroBon = _numeroBon.value,
            tauxCouverturePrincipal = tauxPrincipal,
            tauxCouvertureComplementaires = complementaireCoverageRates.toMap(),
            ayantDroit = _ayantDroit.value
        )
    }

    /**
     * Validate insurance data
     */
    fun validateInsuranceData(): Pair<Boolean, String?> {
        val insuranceData = _tiersPayantPrincipal.value?.let { principal ->
            val prescriptionType = _prescriptionType.value ?: PrescriptionType.ORDONNANCE
            val tauxPrincipal = _tauxCouverturePrincipal.value ?: 0

            InsuranceData(
                tiersPayantPrincipal = principal,
                tiersPayantsComplementaires = _tiersPayantsComplementaires.value ?: emptyList(),
                prescriptionType = prescriptionType,
                numeroBon = _numeroBon.value,
                tauxCouverturePrincipal = tauxPrincipal,
                tauxCouvertureComplementaires = complementaireCoverageRates.toMap()
            )
        }

        val validation = validator.validateInsuranceData(insuranceData)
        return if (validation.isValid) {
            true to null
        } else {
            false to validation.getErrorMessage()
        }
    }

    /**
     * Reset all data
     */
    fun reset() {
        _tiersPayantPrincipal.value = null
        _tiersPayantsComplementaires.value = emptyList()
        _tauxCouverturePrincipal.value = 0
        _prescriptionType.value = PrescriptionType.ORDONNANCE
        _numeroBon.value = null
        _ayantDroit.value = null
        complementaireCoverageRates.clear()
        recalculateCoverageRates()
    }

    // ========== Utility Methods ==========

    fun clearError() {
        _errorMessage.value = null
    }
}
