package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.model.ProductQuickInfo
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Product Detail screen.
 */
class ProductDetailViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    // =========================================================================
    // LIVEDATA
    // =========================================================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _product = MutableLiveData<ProductQuickInfo>()
    val product: LiveData<ProductQuickInfo> = _product

    private var currentProductId: Long? = null

    // =========================================================================
    // ACTIONS
    // =========================================================================

    /**
     * Load product details.
     */
    fun loadProduct(productId: Long) {
        currentProductId = productId
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getProductQuickInfo(productId)

            result.fold(
                onSuccess = { productInfo ->
                    _product.value = productInfo
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Refresh product data (pull-to-refresh).
     */
    fun refreshProduct() {
        currentProductId?.let { productId ->
            viewModelScope.launch {
                _isRefreshing.value = true
                _errorMessage.value = null

                val result = repository.getProductQuickInfo(productId)

                result.fold(
                    onSuccess = { productInfo ->
                        _product.value = productInfo
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )

                _isRefreshing.value = false
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for ProductDetailViewModel.
 */
class ProductDetailViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductDetailViewModel::class.java)) {
            return ProductDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
