package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.TiersPayantRepository

/**
 * Factory for creating InsuranceDataViewModel
 */
class InsuranceDataViewModelFactory(
    private val tiersPayantRepository: TiersPayantRepository,
    private val customerRepository: CustomerRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsuranceDataViewModel::class.java)) {
            return InsuranceDataViewModel(
                tiersPayantRepository,
                customerRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
