package com.kobe.warehouse.inventory.data.api

import com.kobe.warehouse.inventory.data.model.AppConfig
import com.kobe.warehouse.inventory.data.model.BatchSyncResult
import com.kobe.warehouse.inventory.data.model.InventoryLot
import com.kobe.warehouse.inventory.data.model.InventoryLotLine
import com.kobe.warehouse.inventory.data.model.InventoryProgress
import com.kobe.warehouse.inventory.data.model.Product
import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.model.StoreInventoryLineSync
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Inventory API service
 * Aligned with backend StoreInventoryResource / StoreInventoryLineResource
 */
interface InventoryApiService {

    /**
     * List inventories, filterable by status
     * GET /api/store-inventories?statuts=CREATE&statuts=PROCESSING
     */
    @GET("api/store-inventories")
    suspend fun getInventories(
        @Query("statuts") statuts: List<String>,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<List<StoreInventory>>

    /**
     * Get inventory by ID
     * GET /api/store-inventories/{id}
     */
    @GET("api/store-inventories/{id}")
    suspend fun getInventory(@Path("id") id: Long): Response<StoreInventory>

    /**
     * Inventory counting progress
     * GET /api/store-inventories/{id}/progress
     */
    @GET("api/store-inventories/{id}/progress")
    suspend fun getProgress(@Path("id") id: Long): Response<InventoryProgress>

    // Pas d'appel de clôture ici : GET /api/store-inventories/close/{id} est
    // réservé au poste (web / Tauri). Le mobile ne fait que compter et synchroniser.

    /**
     * Paged inventory lines (v2: no N+1, multi-storage stock)
     * GET /api/store-inventory-lines/v2?storeInventoryId=&rayonId=&search=
     */
    @GET("api/store-inventory-lines/v2")
    suspend fun getInventoryLines(
        @Query("storeInventoryId") storeInventoryId: Long,
        @Query("rayonId") rayonId: Long? = null,
        @Query("search") search: String? = null,
        @Query("selectedFilter") selectedFilter: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 500
    ): Response<List<StoreInventoryLine>>

    /**
     * Vue à plat « un lot = une ligne », mode de saisie quand la gestion des lots
     * est active
     * GET /api/store-inventory-lines/lots?storeInventoryId=&rayonId=&search=
     */
    @GET("api/store-inventory-lines/lots")
    suspend fun getInventoryLotLines(
        @Query("storeInventoryId") storeInventoryId: Long,
        @Query("rayonId") rayonId: Long? = null,
        @Query("search") search: String? = null,
        @Query("selectedFilter") selectedFilter: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 500
    ): Response<List<InventoryLotLine>>

    /**
     * Paramètre applicatif — sert à résoudre le mode de saisie
     * GET /api/app/{id}
     */
    @GET("api/app/{id}")
    suspend fun getAppConfig(@Path("id") id: String): Response<AppConfig>

    /**
     * Update a single line quantity
     * PUT /api/store-inventory-lines
     */
    @PUT("api/store-inventory-lines")
    suspend fun updateInventoryLine(
        @Body line: StoreInventoryLineSync
    ): Response<StoreInventoryLine>

    /**
     * Batch synchronization (replaces N unit PUT calls)
     * PUT /api/store-inventory-lines/batch
     */
    @PUT("api/store-inventory-lines/batch")
    suspend fun synchronizeInventoryLines(
        @Body lines: List<StoreInventoryLineSync>
    ): Response<BatchSyncResult>

    /**
     * Rayons referential (optionally filtered by storage)
     * GET /api/rayons?storageId=
     */
    @GET("api/rayons")
    suspend fun getRayons(
        @Query("storageId") storageId: Long? = null,
        @Query("search") search: String = "",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 500
    ): Response<List<Rayon>>

    /**
     * Search product by barcode (CIP/EAN)
     * GET /api/produits/code/{code}
     */
    @GET("api/produits/code/{code}")
    suspend fun searchProductByCode(
        @Path("code") code: String
    ): Response<List<Product>>

    /**
     * Current user's executable ACTION codes (lightweight — no nav tree)
     * GET /api/nav/my-abilities
     */
    @GET("api/nav/my-abilities")
    suspend fun getMyAbilities(): Response<Set<String>>

    /**
     * Lots of an inventory line
     * GET /api/store-inventory-lines/{lineId}/lots
     */
    @GET("api/store-inventory-lines/{lineId}/lots")
    suspend fun getInventoryLots(
        @Path("lineId") lineId: Long
    ): Response<List<InventoryLot>>

    /**
     * Create a lot on an inventory line (existing lotId, or numLot to create)
     * POST /api/store-inventory-lines/{lineId}/lots
     */
    @POST("api/store-inventory-lines/{lineId}/lots")
    suspend fun createInventoryLot(
        @Path("lineId") lineId: Long,
        @Body lot: InventoryLot
    ): Response<InventoryLot>

    /**
     * Update a lot quantity — the backend re-syncs the parent line quantity
     * PUT /api/store-inventory-lines/lots/{lotId}
     */
    @PUT("api/store-inventory-lines/lots/{lotId}")
    suspend fun updateInventoryLot(
        @Path("lotId") lotId: Long,
        @Body lot: InventoryLot
    ): Response<InventoryLot>

    /**
     * Delete an inventory lot
     * DELETE /api/store-inventory-lines/lots/{lotId}
     */
    @DELETE("api/store-inventory-lines/lots/{lotId}")
    suspend fun deleteInventoryLot(
        @Path("lotId") lotId: Long
    ): Response<Void>
}
