package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import com.kobe.warehouse.sales.data.model.Payment
import com.kobe.warehouse.sales.data.model.Sale

/**
 * Common interface for sale ViewModels
 * Allows PaymentDialogFragment to work with both ComptantSaleViewModel and UnifiedSaleViewModel
 */
interface ISaleViewModel {
    val currentSale: LiveData<Sale>

    fun finalizeSale(payments: List<Payment>, montantVerse: Int, montantRendu: Int)
}
