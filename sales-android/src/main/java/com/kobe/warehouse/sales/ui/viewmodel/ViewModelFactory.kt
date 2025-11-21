package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * ViewModelFactory for SalesHomeViewModel
 */
class SalesHomeViewModelFactory(
    private val salesRepository: SalesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesHomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesHomeViewModel(salesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * ViewModelFactory for ComptantSaleViewModel
 */
class ComptantSaleViewModelFactory(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComptantSaleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ComptantSaleViewModel(
                salesRepository,
                productRepository,
                paymentRepository,
                authRepository,
                tokenManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
