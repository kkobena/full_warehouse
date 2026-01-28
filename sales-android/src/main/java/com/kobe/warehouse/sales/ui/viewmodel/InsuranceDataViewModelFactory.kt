package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating InsuranceDataViewModel
 * Note: InsuranceDataViewModel doesn't require repository dependencies as it's primarily used
 * for UI state management. Data fetching is handled by parent UnifiedSaleViewModel.
 */
class InsuranceDataViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsuranceDataViewModel::class.java)) {
            return InsuranceDataViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
