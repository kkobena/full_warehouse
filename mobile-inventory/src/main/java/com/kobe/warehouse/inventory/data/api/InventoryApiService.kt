package com.kobe.warehouse.inventory.data.api

import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import retrofit2.Response
import retrofit2.http.*

/**
 * Inventory API service
 * Handles inventory operations
 * Based on backend InventaireServiceImpl endpoints
 */
interface InventoryApiService {

    /**
     * Get all active inventories
     * GET /api/store-inventories/actif
     */
    @GET("api/store-inventories/actif")
    suspend fun getActiveInventories(): Response<List<StoreInventory>>

    /**
     * Get inventory by ID
     * GET /api/store-inventories/{id}
     */
    @GET("api/store-inventories/{id}")
    suspend fun getInventory(@Path("id") id: Long): Response<StoreInventory>

    /**
     * Get inventory items (lines) by rayon
     * GET /api/store-inventories/{inventoryId}/rayons/{rayonId}/items
     */
    @GET("api/store-inventories/{inventoryId}/rayons/{rayonId}/items")
    suspend fun getInventoryItemsByRayon(
        @Path("inventoryId") inventoryId: Long,
        @Path("rayonId") rayonId: Long
    ): Response<List<StoreInventoryLine>>

    /**
     * Get all rayons for an inventory
     * GET /api/store-inventories/{inventoryId}/rayons
     */
    @GET("api/store-inventories/{inventoryId}/rayons")
    suspend fun getInventoryRayons(
        @Path("inventoryId") inventoryId: Long
    ): Response<List<Rayon>>

    /**
     * Update inventory line quantity
     * PUT /api/store-inventories/lines/{id}
     */
    @PUT("api/store-inventories/lines/{id}")
    suspend fun updateInventoryLine(
        @Path("id") lineId: Long,
        @Body line: StoreInventoryLine
    ): Response<StoreInventoryLine>

    /**
     * Synchronize inventory lines (batch update)
     * PUT /api/store-inventories/lines
     */
    @PUT("api/store-inventories/lines")
    suspend fun synchronizeInventoryLines(
        @Body lines: List<StoreInventoryLine>
    ): Response<Void>

    /**
     * Search product by barcode
     * GET /api/products/code/{code}
     */
    @GET("api/products/code/{code}")
    suspend fun searchProductByCode(
        @Path("code") code: String
    ): Response<com.kobe.warehouse.inventory.data.model.Product>

    /**
     * Close inventory
     * POST /api/store-inventories/close/{id}
     */
    @POST("api/store-inventories/close/{id}")
    suspend fun closeInventory(
        @Path("id") id: Long
    ): Response<Void>
}
