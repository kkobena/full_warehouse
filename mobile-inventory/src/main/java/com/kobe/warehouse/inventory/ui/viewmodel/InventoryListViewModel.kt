package com.kobe.warehouse.inventory.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import kotlinx.coroutines.launch

sealed class InventoryListState {
    object Idle : InventoryListState()
    object Loading : InventoryListState()
    data class Success(val inventories: List<StoreInventory>) : InventoryListState()
    data class Error(val message: String) : InventoryListState()
}

class InventoryListViewModel(private val inventoryRepository: InventoryRepository) : ViewModel() {

    private val _inventoryListState = MutableLiveData<InventoryListState>(InventoryListState.Idle)
    val inventoryListState: LiveData<InventoryListState> = _inventoryListState

    fun loadActiveInventories() {
        viewModelScope.launch {
            _inventoryListState.value = InventoryListState.Loading
            inventoryRepository.getActiveInventories().fold(
                onSuccess = { inventories ->
                    _inventoryListState.value = InventoryListState.Success(inventories)
                },
                onFailure = { error ->
                    _inventoryListState.value = InventoryListState.Error(
                        error.message ?: "Erreur lors du chargement des inventaires"
                    )
                }
            )
        }
    }

    fun refreshInventories() {
        loadActiveInventories()
    }
}
