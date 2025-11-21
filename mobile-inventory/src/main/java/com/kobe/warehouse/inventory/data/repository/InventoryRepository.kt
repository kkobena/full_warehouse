package com.kobe.warehouse.inventory.data.repository

import android.util.Log
import com.kobe.warehouse.inventory.data.api.InventoryApiService
import com.kobe.warehouse.inventory.data.model.Product
import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.utils.ApiClient
import com.kobe.warehouse.inventory.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Inventory Repository
 * Handles inventory operations and caching
 */
class InventoryRepository(private val tokenManager: TokenManager) {

    companion object {
        private const val TAG = "InventoryRepository"
    }

    private val apiService: InventoryApiService by lazy {
        ApiClient.create(tokenManager = tokenManager).create(InventoryApiService::class.java)
    }

    /**
     * Get all active inventories
     */
    suspend fun getActiveInventories(): Result<List<StoreInventory>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getActiveInventories()

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Loaded ${response.body()!!.size} active inventories")
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = "Failed to load inventories: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading inventories", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get inventory by ID
     */
    suspend fun getInventory(id: Long): Result<StoreInventory> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInventory(id)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load inventory: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get inventory rayons (sections)
     */
    suspend fun getInventoryRayons(inventoryId: Long): Result<List<Rayon>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInventoryRayons(inventoryId)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Loaded ${response.body()!!.size} rayons for inventory $inventoryId")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load rayons: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading rayons", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get inventory items by rayon
     */
    suspend fun getInventoryItemsByRayon(inventoryId: Long, rayonId: Long): Result<List<StoreInventoryLine>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInventoryItemsByRayon(inventoryId, rayonId)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Loaded ${response.body()!!.size} items for rayon $rayonId")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load items: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading items", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Search product by barcode
     */
    suspend fun searchProductByBarcode(barcode: String): Result<Product> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchProductByCode(barcode)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Found product: ${response.body()!!.productName}")
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = when (response.code()) {
                        404 -> "Produit non trouvé"
                        else -> "Erreur de recherche: ${response.code()}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching product", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update single inventory line
     */
    suspend fun updateInventoryLine(line: StoreInventoryLine): Result<StoreInventoryLine> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateInventoryLine(line.id, line)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Updated inventory line: ${line.id}")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to update line: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating line", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Synchronize multiple inventory lines (batch update)
     */
    suspend fun synchronizeInventoryLines(lines: List<StoreInventoryLine>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.synchronizeInventoryLines(lines)

                if (response.isSuccessful) {
                    Log.d(TAG, "Synchronized ${lines.size} inventory lines")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to synchronize: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error synchronizing lines", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Close inventory
     */
    suspend fun closeInventory(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.closeInventory(id)

                if (response.isSuccessful) {
                    Log.d(TAG, "Closed inventory: $id")
                    Result.success(Unit)
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Inventaire incomplet - tous les articles doivent être comptés"
                        else -> "Erreur de clôture: ${response.code()}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing inventory", e)
                Result.failure(e)
            }
        }
    }
}
