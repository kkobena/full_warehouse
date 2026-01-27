package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.sales.data.repository.CustomerRepository

/**
 * Factory for creating CarnetSaleViewModel
 */
class CarnetSaleViewModelFactory(
    private val customerRepository: CustomerRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarnetSaleViewModel::class.java)) {
            return CarnetSaleViewModel(customerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
