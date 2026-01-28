package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating CarnetSaleViewModel
 * Note: CarnetSaleViewModel doesn't require repository dependencies as it's primarily used
 * for UI state management. Data fetching is handled by parent UnifiedSaleViewModel.
 */
class CarnetSaleViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarnetSaleViewModel::class.java)) {
            return CarnetSaleViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
