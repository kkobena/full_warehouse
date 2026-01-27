package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Factory for creating UnifiedSaleViewModel
 */
class UnifiedSaleViewModelFactory(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UnifiedSaleViewModel::class.java)) {
            return UnifiedSaleViewModel(
                salesRepository,
                productRepository,
                customerRepository,
                paymentRepository,
                authRepository,
                tokenManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
