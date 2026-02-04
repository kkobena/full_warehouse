package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.User
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * FullSaleHomeViewModel
 * Manages state for full sales home screen with tabs (Ventes en cours + Preventes)
 *
 * Features:
 * - Load ongoing sales (ventes en cours)
 * - Load preventes (sales on hold)
 * - Search/filter support
 * - User filter (Spinner per tab)
 * - Delete sales
 */
class FullSaleHomeViewModel(
    private val salesRepository: SalesRepository,
    private val userRepository: UserRepository,
    val defaultUserId: Int? = null
) : ViewModel() {

    // Ongoing sales state
    private val _ongoingSales = MutableLiveData<List<Sale>>()
    val ongoingSales: LiveData<List<Sale>> = _ongoingSales

    // Preventes state
    private val _preventes = MutableLiveData<List<Sale>>()
    val preventes: LiveData<List<Sale>> = _preventes

    // Loading state
    private val _isLoadingOngoing = MutableLiveData<Boolean>()
    val isLoadingOngoing: LiveData<Boolean> = _isLoadingOngoing

    private val _isLoadingPreventes = MutableLiveData<Boolean>()
    val isLoadingPreventes: LiveData<Boolean> = _isLoadingPreventes

    // Error state
    private val _ongoingError = MutableLiveData<String?>()
    val ongoingError: LiveData<String?> = _ongoingError

    private val _preventesError = MutableLiveData<String?>()
    val preventesError: LiveData<String?> = _preventesError

    // Users list for Spinner
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    // Search query
    private var currentOngoingSearch: String? = null
    private var currentPreventesSearch: String? = null

    // Current user filter per tab
    private var currentOngoingUserId: Int? = defaultUserId
    private var currentPreventesUserId: Int? = defaultUserId

    // Selected sale for navigation
    private val _selectedSale = MutableLiveData<Sale?>()
    val selectedSale: LiveData<Sale?> = _selectedSale

    // Event to switch to vente en cours tab with a sale to edit
    private val _navigateToVenteEnCours = MutableLiveData<NavigateToSaleEvent?>()
    val navigateToVenteEnCours: LiveData<NavigateToSaleEvent?> = _navigateToVenteEnCours

    /**
     * Data class for navigation event
     */
    data class NavigateToSaleEvent(
        val saleId: Long,
        val saleDate: String,
        val natureVente: String
    )

    init {
        loadUsers()
        loadOngoingSales(userId = defaultUserId)
        loadPreventes(userId = defaultUserId)
    }

    /**
     * Load users list for Spinner
     */
    fun loadUsers() {
        viewModelScope.launch {
            userRepository.getUsers().fold(
                onSuccess = { userList ->
                    _users.value = userList
                },
                onFailure = {
                    _users.value = emptyList()
                }
            )
        }
    }

    /**
     * Load ongoing sales (ventes en cours)
     */
    fun loadOngoingSales(search: String? = null, userId: Int? = currentOngoingUserId) {
        currentOngoingSearch = search
        currentOngoingUserId = userId

        viewModelScope.launch {
            _isLoadingOngoing.value = true
            _ongoingError.value = null

            salesRepository.getVenteEncours(search, userId).fold(
                onSuccess = { salesList ->
                    _ongoingSales.value = salesList
                    _isLoadingOngoing.value = false
                },
                onFailure = { error ->
                    _ongoingError.value = error.message ?: "Erreur de chargement des ventes"
                    _isLoadingOngoing.value = false
                }
            )
        }
    }

    /**
     * Load preventes (ventes mises en attente)
     */
    fun loadPreventes(search: String? = null, userId: Int? = currentPreventesUserId) {
        currentPreventesSearch = search
        currentPreventesUserId = userId

        viewModelScope.launch {
            _isLoadingPreventes.value = true
            _preventesError.value = null

            salesRepository.getPreventes(search, userId).fold(
                onSuccess = { salesList ->
                    _preventes.value = salesList
                    _isLoadingPreventes.value = false
                },
                onFailure = { error ->
                    _preventesError.value = error.message ?: "Erreur de chargement des preventes"
                    _isLoadingPreventes.value = false
                }
            )
        }
    }

    /**
     * Refresh ongoing sales
     */
    fun refreshOngoingSales() {
        loadOngoingSales(currentOngoingSearch, currentOngoingUserId)
    }

    /**
     * Refresh preventes
     */
    fun refreshPreventes() {
        loadPreventes(currentPreventesSearch, currentPreventesUserId)
    }

    /**
     * Search in ongoing sales
     */
    fun searchOngoingSales(query: String) {
        loadOngoingSales(query, currentOngoingUserId)
    }

    /**
     * Search in preventes
     */
    fun searchPreventes(query: String) {
        loadPreventes(query, currentPreventesUserId)
    }

    /**
     * Delete sale
     */
    fun deleteSale(saleId: Long, saleDate: String, isPrevente: Boolean = false) {
        viewModelScope.launch {
            salesRepository.deleteSale(saleId, saleDate).fold(
                onSuccess = {
                    if (isPrevente) {
                        refreshPreventes()
                    } else {
                        refreshOngoingSales()
                    }
                },
                onFailure = { error ->
                    if (isPrevente) {
                        _preventesError.value = error.message ?: "Erreur de suppression"
                    } else {
                        _ongoingError.value = error.message ?: "Erreur de suppression"
                    }
                }
            )
        }
    }

    /**
     * Select sale for navigation
     */
    fun selectSale(sale: Sale) {
        _selectedSale.value = sale
    }

    /**
     * Clear selected sale
     */
    fun clearSelectedSale() {
        _selectedSale.value = null
    }

    /**
     * Clear errors
     */
    fun clearOngoingError() {
        _ongoingError.value = null
    }

    fun clearPreventesError() {
        _preventesError.value = null
    }

    /**
     * Transform a prevente (PENDING) to vente en cours (PROCESSING)
     * and navigate to the vente en cours tab
     */
    fun transformPreventeToVenteEnCours(saleId: Long, saleDate: String, natureVente: String) {
        viewModelScope.launch {
            // Call API to change status from PENDING to PROCESSING
            salesRepository.activatePrevente(saleId, saleDate).fold(
                onSuccess = {
                    // Emit navigation event - the activity will switch tabs and open the sale
                    _navigateToVenteEnCours.value = NavigateToSaleEvent(saleId, saleDate, natureVente)

                    // Refresh preventes list (to remove the transformed sale)
                    refreshPreventes()

                    // Refresh ongoing sales list (to show the transformed sale)
                    refreshOngoingSales()
                },
                onFailure = { error ->
                    _preventesError.value = "Erreur de transformation: ${error.message}"
                }
            )
        }
    }

    /**
     * Clear navigation event
     */
    fun clearNavigateToVenteEnCours() {
        _navigateToVenteEnCours.value = null
    }
}
