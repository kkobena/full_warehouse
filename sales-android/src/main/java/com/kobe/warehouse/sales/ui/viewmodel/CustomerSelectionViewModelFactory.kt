package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.sales.data.repository.CustomerRepository

/**
 * Factory for creating CustomerSelectionViewModel with dependencies
 */
class CustomerSelectionViewModelFactory(
    private val customerRepository: CustomerRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerSelectionViewModel::class.java)) {
            return CustomerSelectionViewModel(customerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
