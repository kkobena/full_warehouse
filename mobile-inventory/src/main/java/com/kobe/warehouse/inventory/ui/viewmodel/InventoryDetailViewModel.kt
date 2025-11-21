package com.kobe.warehouse.inventory.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.inventory.data.model.Product
import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import kotlinx.coroutines.launch

sealed class InventoryDetailState {
    object Idle : InventoryDetailState()
    object Loading : InventoryDetailState()
    data class InventoryLoaded(val inventory: StoreInventory) : InventoryDetailState()
    data class RayonsLoaded(val rayons: List<Rayon>) : InventoryDetailState()
    data class LinesLoaded(val lines: List<StoreInventoryLine>) : InventoryDetailState()
    data class ProductFound(val product: Product) : InventoryDetailState()
    object LineSaved : InventoryDetailState()
    object SyncSuccess : InventoryDetailState()
    object InventoryClosed : InventoryDetailState()
    data class Error(val message: String) : InventoryDetailState()
}

class InventoryDetailViewModel(private val inventoryRepository: InventoryRepository) : ViewModel() {

    private val _inventoryDetailState = MutableLiveData<InventoryDetailState>(InventoryDetailState.Idle)
    val inventoryDetailState: LiveData<InventoryDetailState> = _inventoryDetailState

    private var currentInventoryId: Long? = null
    private var currentRayonId: Long? = null
    private var currentLines = mutableListOf<StoreInventoryLine>()

    fun loadInventory(inventoryId: Long) {
        currentInventoryId = inventoryId
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.getInventoryById(inventoryId).fold(
                onSuccess = { inventory ->
                    _inventoryDetailState.value = InventoryDetailState.InventoryLoaded(inventory)
                    // Auto-load rayons if applicable
                    loadRayons(inventoryId)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors du chargement de l'inventaire"
                    )
                }
            )
        }
    }

    fun loadRayons(inventoryId: Long) {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.getInventoryRayons(inventoryId).fold(
                onSuccess = { rayons ->
                    _inventoryDetailState.value = InventoryDetailState.RayonsLoaded(rayons)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors du chargement des rayons"
                    )
                }
            )
        }
    }

    fun loadInventoryLines(inventoryId: Long, rayonId: Long?) {
        currentInventoryId = inventoryId
        currentRayonId = rayonId
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            val result = if (rayonId != null) {
                inventoryRepository.getInventoryLinesByRayon(inventoryId, rayonId)
            } else {
                // Load all lines if no rayon specified
                Result.success(emptyList())
            }

            result.fold(
                onSuccess = { lines ->
                    currentLines.clear()
                    currentLines.addAll(lines)
                    _inventoryDetailState.value = InventoryDetailState.LinesLoaded(lines)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors du chargement des lignes"
                    )
                }
            )
        }
    }

    fun searchProductByBarcode(barcode: String) {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.searchProductByBarcode(barcode).fold(
                onSuccess = { product ->
                    _inventoryDetailState.value = InventoryDetailState.ProductFound(product)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Produit non trouvé"
                    )
                }
            )
        }
    }

    fun updateInventoryLine(line: StoreInventoryLine) {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.updateInventoryLine(line).fold(
                onSuccess = {
                    // Update local cache
                    val index = currentLines.indexOfFirst { it.id == line.id }
                    if (index >= 0) {
                        currentLines[index] = line
                    } else {
                        currentLines.add(line)
                    }
                    _inventoryDetailState.value = InventoryDetailState.LineSaved
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la sauvegarde"
                    )
                }
            )
        }
    }

    fun synchronizeLines() {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.synchronizeInventoryLines(currentLines).fold(
                onSuccess = {
                    _inventoryDetailState.value = InventoryDetailState.SyncSuccess
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur de synchronisation"
                    )
                }
            )
        }
    }

    fun closeInventory(inventoryId: Long) {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.closeInventory(inventoryId).fold(
                onSuccess = {
                    _inventoryDetailState.value = InventoryDetailState.InventoryClosed
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la clôture"
                    )
                }
            )
        }
    }

    fun getCurrentLines(): List<StoreInventoryLine> = currentLines
}
