package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.DeconditionRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Factory for creating UnifiedSaleViewModel with dependencies
 */
class UnifiedSaleViewModelFactory(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val deconditionRepository: DeconditionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UnifiedSaleViewModel::class.java)) {
            return UnifiedSaleViewModel(
                salesRepository,
                productRepository,
                paymentRepository,
                customerRepository,
                authRepository,
                tokenManager,
                deconditionRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
