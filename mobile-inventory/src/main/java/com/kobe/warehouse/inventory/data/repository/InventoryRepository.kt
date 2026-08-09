package com.kobe.warehouse.inventory.data.repository

import android.content.Context
import android.util.Log
import com.kobe.warehouse.inventory.data.api.InventoryApiService
import com.kobe.warehouse.inventory.data.database.InventoryDatabase
import com.kobe.warehouse.inventory.data.database.entity.InventoryEntity
import com.kobe.warehouse.inventory.data.database.entity.InventoryLineEntity
import com.kobe.warehouse.inventory.data.database.entity.InventoryLotLineEntity
import com.kobe.warehouse.inventory.data.model.BatchSyncResult
import com.kobe.warehouse.inventory.data.model.InventoryLot
import com.kobe.warehouse.inventory.data.model.InventoryLotLine
import com.kobe.warehouse.inventory.data.model.InventoryProgress
import com.kobe.warehouse.inventory.data.model.InventoryStatut
import com.kobe.warehouse.inventory.data.model.Product
import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.model.StoreInventoryLineSync
import com.kobe.warehouse.inventory.sync.SyncManager
import com.kobe.warehouse.inventory.utils.ApiClient
import com.kobe.warehouse.inventory.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Result of a local-first line save: the line as persisted, and whether it
 * reached the server (false = stored locally, pending sync)
 */
data class LineSaveResult(
    val line: StoreInventoryLine,
    val synced: Boolean
)

/** Equivalent pour un lot compte depuis la vue a plat */
data class LotLineSaveResult(
    val line: InventoryLotLine,
    val synced: Boolean
)

/** Levée quand le serveur rejette une saisie calculée sur une version périmée (HTTP 409) */
class CountingConflictException(message: String) : Exception(message)

/**
 * Inventory Repository — offline-first.
 * Reads try the network then fall back to the Room cache; line writes are
 * persisted locally first (source of truth), then pushed to the server.
 */
class InventoryRepository(context: Context) {

    companion object {
        private const val TAG = "InventoryRepository"
        private const val PAGE_SIZE = 500
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_SYNCED = "SYNCED"
        private const val STATUS_ERROR = "ERROR"
        private const val STATUS_CONFLICT = "CONFLICT"
        private const val CONFIG_GESTION_LOT = "APP_GESTION_LOT_INVENTAIRE"
    }

    private val appContext = context.applicationContext
    private val tokenManager = TokenManager(appContext)
    private val database = InventoryDatabase.getInstance(appContext)
    private val inventoryDao = database.inventoryDao()
    private val lineDao = database.inventoryLineDao()
    private val lotLineDao = database.inventoryLotLineDao()

    private val apiService: InventoryApiService by lazy {
        ApiClient.create(tokenManager = tokenManager).create(InventoryApiService::class.java)
    }

    /**
     * Number of locally modified lines waiting to be pushed (PENDING or ERROR)
     */
    fun pendingSyncCount(): Flow<Int> =
        lineDao.getPendingSyncCount().combine(lotLineDao.getPendingSyncCount()) { a, b -> a + b }

    /** Lignes saisies mais pas encore transmises, pour les marquer dans la liste */
    fun pendingSyncIds(): Flow<List<Long>> = lineDao.getPendingSyncIds()

    /** Idem pour les lots (mode gestion des lots) */
    fun pendingLotSyncIds(): Flow<List<Long>> = lotLineDao.getPendingSyncIds()

    /**
     * Lignes rejetées pour comptage concurrent — à ré-arbitrer par l'opérateur
     */
    fun conflictCount(): Flow<Int> = lineDao.getConflictCount()

    /** Après arbitrage : les lignes en conflit sont rechargées depuis le serveur */
    suspend fun resolveConflicts(inventoryId: Long) {
        withContext(Dispatchers.IO) {
            lineDao.clearConflicts()
        }
        getInventoryLines(inventoryId)
    }

    /**
     * ACTION permissions of the current user (nav_item canExecute codes).
     * Network first, persisted for offline reuse.
     */
    suspend fun fetchAbilities(): Result<Set<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMyAbilities()
                if (response.isSuccessful && response.body() != null) {
                    val abilities = response.body()!!
                    tokenManager.saveAbilities(abilities)
                    Log.d(TAG, "Loaded ${abilities.size} executable abilities")
                    Result.success(abilities)
                } else {
                    Result.success(tokenManager.getAbilities())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network unavailable, using cached abilities", e)
                Result.success(tokenManager.getAbilities())
            }
        }
    }

    /**
     * Get all active (not closed) inventories — network first, cache fallback
     */
    suspend fun getActiveInventories(): Result<List<StoreInventory>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInventories(
                    statuts = listOf(
                        InventoryStatut.CREATE.name,
                        InventoryStatut.PROCESSING.name
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val inventories = response.body()!!
                    inventoryDao.upsertInventories(inventories.map { InventoryEntity.fromModel(it) })
                    Log.d(TAG, "Loaded ${inventories.size} active inventories")
                    Result.success(inventories)
                } else {
                    val errorMsg = "Failed to load inventories: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network unavailable, loading inventories from cache", e)
                val cached = inventoryDao.getActiveInventoriesOnce()
                if (cached.isNotEmpty()) {
                    Result.success(cached.map { it.toModel() })
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Get inventory by ID — network first, cache fallback
     */
    suspend fun getInventory(id: Long): Result<StoreInventory> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInventory(id)

                if (response.isSuccessful && response.body() != null) {
                    val inventory = response.body()!!
                    inventoryDao.upsertInventory(InventoryEntity.fromModel(inventory))
                    Result.success(inventory)
                } else {
                    Result.failure(Exception("Failed to load inventory: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network unavailable, loading inventory $id from cache", e)
                val cached = inventoryDao.getInventoryById(id)
                if (cached != null) {
                    Result.success(cached.toModel())
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Get inventory counting progress (network only — non-blocking usage)
     */
    suspend fun getProgress(id: Long): Result<InventoryProgress> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getProgress(id)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load progress: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get rayons (optionally restricted to the inventory's storage)
     */
    suspend fun getRayons(storageId: Long? = null): Result<List<Rayon>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRayons(storageId = storageId)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Loaded ${response.body()!!.size} rayons (storageId=$storageId)")
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
     * Get inventory lines. Network first (paged), merged into the Room cache
     * without overwriting locally modified pending lines; falls back to the
     * cache when offline.
     */
    suspend fun getInventoryLines(
        inventoryId: Long,
        rayonId: Long? = null,
        search: String? = null,
        selectedFilter: String? = null
    ): Result<List<StoreInventoryLine>> {
        return withContext(Dispatchers.IO) {
            try {
                val allLines = mutableListOf<StoreInventoryLine>()
                var page = 0
                while (true) {
                    val response = apiService.getInventoryLines(
                        storeInventoryId = inventoryId,
                        rayonId = rayonId,
                        search = search,
                        selectedFilter = selectedFilter?.takeIf { it != "NONE" },
                        page = page,
                        size = PAGE_SIZE
                    )

                    if (!response.isSuccessful || response.body() == null) {
                        return@withContext Result.failure(
                            Exception("Failed to load lines: ${response.code()}")
                        )
                    }

                    val lines = response.body()!!
                    allLines.addAll(lines)
                    if (lines.size < PAGE_SIZE) break
                    page++
                }

                // Merge into cache: locally modified pending lines win over the server
                ensureInventoryCached(inventoryId)
                val unsyncedIds = lineDao.getUnsyncedLines().map { it.id }.toSet()
                lineDao.upsertLines(
                    allLines
                        .filter { it.id !in unsyncedIds }
                        .map { InventoryLineEntity.fromModel(it, inventoryId, rayonId) }
                )
                val merged = allLines.map { remote ->
                    if (remote.id in unsyncedIds) {
                        lineDao.getInventoryLineById(remote.id)?.toModel() ?: remote
                    } else {
                        remote
                    }
                }
                Log.d(TAG, "Loaded ${merged.size} lines for inventory $inventoryId (rayonId=$rayonId)")
                Result.success(merged)
            } catch (e: Exception) {
                Log.w(TAG, "Network unavailable, loading lines from cache", e)
                loadLinesFromCache(inventoryId, rayonId, search, selectedFilter)
            }
        }
    }

    /**
     * Lignes de la vue à plat (un lot = une ligne), paginées comme la vue produit.
     *
     * Pas de cache Room ici : le comptage par lot passe par l'API lot par lot
     * (`PUT /store-inventory-lines/lots/{id}`) et n'a jamais été gréé pour le mode
     * hors ligne. Une panne réseau remonte donc une erreur au lieu d'un silence.
     */
    suspend fun getInventoryLotLines(
        inventoryId: Long,
        rayonId: Long? = null,
        search: String? = null,
        selectedFilter: String? = null
    ): Result<List<InventoryLotLine>> {
        return withContext(Dispatchers.IO) {
            try {
                ensureInventoryCached(inventoryId)
                val all = mutableListOf<InventoryLotLine>()
                var page = 0
                while (true) {
                    val response = apiService.getInventoryLotLines(
                        storeInventoryId = inventoryId,
                        rayonId = rayonId,
                        search = search,
                        selectedFilter = selectedFilter?.takeIf { it != "NONE" },
                        page = page,
                        size = PAGE_SIZE
                    )
                    if (!response.isSuccessful || response.body() == null) {
                        return@withContext Result.failure(
                            Exception("Failed to load lot lines: ${response.code()}")
                        )
                    }
                    val lines = response.body()!!
                    all.addAll(lines)
                    if (lines.size < PAGE_SIZE) break
                    page++
                }
                // Fusion dans le cache : les saisies locales non transmises priment
                val unsyncedIds = lotLineDao.getUnsyncedLotLines().map { it.id }.toSet()
                lotLineDao.upsertLotLines(
                    all.filter { it.id !in unsyncedIds }
                        .map { InventoryLotLineEntity.fromModel(it, inventoryId, rayonId) }
                )
                val merged = all.map { remote ->
                    val cached = remote.id?.takeIf { it in unsyncedIds }
                        ?.let { lotLineDao.getLotLineById(it) }
                    cached?.toModel() ?: remote
                }
                Result.success(applyPendingLineCounts(merged))
            } catch (e: Exception) {
                Log.w(TAG, "Network unavailable, loading lot lines from cache", e)
                loadLotLinesFromCache(inventoryId, rayonId, search, selectedFilter)
            }
        }
    }

    private suspend fun loadLotLinesFromCache(
        inventoryId: Long,
        rayonId: Long?,
        search: String?,
        selectedFilter: String?
    ): Result<List<InventoryLotLine>> {
        val entities = if (rayonId != null) {
            lotLineDao.getLotLinesByRayonOnce(inventoryId, rayonId)
        } else {
            lotLineDao.getLotLinesOnce(inventoryId)
        }
        val query = search?.trim().orEmpty()
        val searched = if (query.isEmpty()) entities else entities.filter { e ->
            listOfNotNull(e.produitLibelle, e.produitCip, e.numLot)
                .any { it.contains(query, ignoreCase = true) }
        }
        // Replique locale du selectedFilter backend
        val filtered = when (selectedFilter) {
            "UPDATED" -> searched.filter { it.updated }
            "NOT_UPDATED" -> searched.filter { !it.updated }
            "GAP" -> searched.filter { (it.gap ?: 0) != 0 }
            "GAP_POSITIF" -> searched.filter { (it.gap ?: 0) > 0 }
            "GAP_NEGATIF" -> searched.filter { (it.gap ?: 0) < 0 }
            else -> searched
        }
        return Result.success(applyPendingLineCounts(filtered.map { it.toModel() }))
    }

    /**
     * Réapplique les comptages en attente des lignes sans lot.
     *
     * Ces lignes s'écrivent par l'API ligne produit, donc leur saisie non transmise vit dans
     * le cache des lignes et non dans celui des lots. Sans ce recollement, une quantité saisie
     * hors ligne disparaîtrait de la grille au premier rechargement et serait recomptée.
     */
    private suspend fun applyPendingLineCounts(
        lines: List<InventoryLotLine>
    ): List<InventoryLotLine> {
        if (lines.none { it.isLotLess() }) return lines
        val pending = lineDao.getUnsyncedLines().associateBy { it.id }
        if (pending.isEmpty()) return lines
        return lines.map { line ->
            val local = line.takeIf { it.isLotLess() }
                ?.storeInventoryLineId
                ?.let { pending[it] }
                ?: return@map line
            line.copy(
                quantityOnHand = local.quantityOnHand,
                quantityInit = local.quantityInit,
                gap = local.gap,
                updated = local.updated,
                version = local.version
            )
        }
    }

    /**
     * Comptage d'un lot — local d'abord, comme la saisie par produit : la quantite
     * est persistee puis poussee. Une coupure reseau ne fait plus perdre la saisie.
     */
    suspend fun saveLotLineQuantity(
        inventoryId: Long,
        lotLine: InventoryLotLine,
        quantity: Int
    ): Result<LotLineSaveResult> {
        val lotId = lotLine.id ?: return Result.failure(Exception("Lot sans identifiant"))
        return withContext(Dispatchers.IO) {
            try {
                ensureInventoryCached(inventoryId)
                val existing = lotLineDao.getLotLineById(lotId)
                val local = (existing ?: InventoryLotLineEntity.fromModel(lotLine, inventoryId))
                    .copy(
                        quantityOnHand = quantity,
                        gap = quantity - (lotLine.quantityInit ?: 0),
                        updated = true,
                        locallyModified = true,
                        syncStatus = STATUS_PENDING
                    )
                lotLineDao.upsertLotLine(local)

                try {
                    val response = apiService.updateInventoryLot(lotId, local.toSyncPayload())
                    if (response.isSuccessful && response.body() != null) {
                        val saved = response.body()!!
                        val synced = local.copy(
                            quantityOnHand = saved.quantityOnHand ?: quantity,
                            gap = saved.gap ?: local.gap,
                            locallyModified = false,
                            syncStatus = STATUS_SYNCED
                        )
                        lotLineDao.upsertLotLine(synced)
                        return@withContext Result.success(
                            LotLineSaveResult(synced.toModel(), synced = true)
                        )
                    }
                    Log.w(TAG, "Lot push refused (${response.code()}), kept pending")
                } catch (e: Exception) {
                    Log.w(TAG, "Lot push failed, kept pending", e)
                }
                SyncManager.syncNow(appContext)
                Result.success(LotLineSaveResult(local.toModel(), synced = false))
            } catch (e: Exception) {
                Log.e(TAG, "Error saving lot quantity", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Transmission des lots comptes hors ligne. Le backend n'expose pas d'ecriture
     * groupee pour les lots : on rejoue les ecritures unitaires, en marquant en
     * erreur celles qui echouent pour les reprendre au prochain passage.
     */
    suspend fun syncPendingLotLines(): Result<Int> = withContext(Dispatchers.IO) {
        val pending = lotLineDao.getUnsyncedLotLines()
        if (pending.isEmpty()) return@withContext Result.success(0)
        var saved = 0
        pending.forEach { entity ->
            try {
                val response = apiService.updateInventoryLot(entity.id, entity.toSyncPayload())
                if (response.isSuccessful) {
                    lotLineDao.updateLotLine(
                        entity.copy(locallyModified = false, syncStatus = STATUS_SYNCED)
                    )
                    saved++
                } else {
                    lotLineDao.updateLotLine(entity.copy(syncStatus = STATUS_ERROR))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lot ${entity.id} still unreachable", e)
            }
        }
        Result.success(saved)
    }

    /**
     * Mode de saisie : lot par lot si APP_GESTION_LOT_INVENTAIRE est actif.
     * Config indisponible → saisie par produit, le mode le plus tolérant.
     */
    suspend fun isGestionLotEnabled(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAppConfig(CONFIG_GESTION_LOT)
            response.body()?.isEnabled() ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Configuration gestion des lots indisponible", e)
            false
        }
    }

    private suspend fun loadLinesFromCache(
        inventoryId: Long,
        rayonId: Long?,
        search: String?,
        selectedFilter: String?
    ): Result<List<StoreInventoryLine>> {
        val entities = if (rayonId != null) {
            lineDao.getInventoryLinesByRayonOnce(inventoryId, rayonId)
        } else {
            lineDao.getInventoryLinesOnce(inventoryId)
        }
        val query = search?.trim().orEmpty()
        val searched = if (query.isEmpty()) entities else entities.filter { e ->
            listOfNotNull(e.produitLibelle, e.produitCip, e.produitEan)
                .any { it.contains(query, ignoreCase = true) }
        }
        // Réplique locale du selectedFilter backend (StoreInventoryLineEnum)
        val filtered = when (selectedFilter) {
            "UPDATED" -> searched.filter { it.updated }
            "NOT_UPDATED" -> searched.filter { !it.updated }
            "GAP" -> searched.filter { (it.gap ?: 0) != 0 }
            "GAP_POSITIF" -> searched.filter { (it.gap ?: 0) > 0 }
            "GAP_NEGATIF" -> searched.filter { (it.gap ?: 0) < 0 }
            else -> searched
        }
        return Result.success(filtered.map { it.toModel() })
    }

    /**
     * Save a line quantity — local first (never fails for network reasons),
     * then immediate push to the server. If the push fails the line stays
     * PENDING and a background sync is scheduled.
     */
    suspend fun saveLineQuantity(
        inventoryId: Long,
        line: StoreInventoryLine,
        quantity: Int
    ): Result<LineSaveResult> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Persist locally (source of truth)
                ensureInventoryCached(inventoryId)
                val existing = lineDao.getInventoryLineById(line.id)
                val localEntity = (existing ?: InventoryLineEntity.fromModel(line, inventoryId))
                    .copy(
                        quantityOnHand = quantity,
                        gap = quantity - line.quantityInit,
                        updated = true,
                        locallyModified = true,
                        syncStatus = STATUS_PENDING
                    )
                lineDao.upsertLine(localEntity)

                // 2. Try to push immediately
                try {
                    val response = apiService.updateInventoryLine(localEntity.toSyncPayload())
                    if (response.isSuccessful && response.body() != null) {
                        val serverLine = response.body()!!
                        lineDao.upsertLine(
                            InventoryLineEntity.fromModel(serverLine, inventoryId, existing?.rayonId)
                                .copy(locallyModified = false, syncStatus = STATUS_SYNCED)
                        )
                        return@withContext Result.success(LineSaveResult(serverLine, synced = true))
                    }
                    // 409 : un autre opérateur a compté cette ligne entre-temps.
                    // On ne réessaie pas en tâche de fond — la saisie doit être arbitrée.
                    if (response.code() == 409) {
                        Log.w(TAG, "Counting conflict on line ${line.id}")
                        lineDao.upsertLine(localEntity.copy(syncStatus = STATUS_CONFLICT))
                        return@withContext Result.failure(
                            CountingConflictException(
                                "Cette ligne vient d'être comptée par un autre opérateur"
                            )
                        )
                    }
                    Log.w(TAG, "Line push rejected (${response.code()}), kept pending")
                } catch (e: Exception) {
                    Log.w(TAG, "Line push failed, kept pending for background sync", e)
                }

                // 3. Offline or push failed: schedule sync, report local success
                SyncManager.syncNow(appContext)
                Result.success(LineSaveResult(localEntity.toModel(), synced = false))
            } catch (e: Exception) {
                Log.e(TAG, "Error saving line locally", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Push locally modified lines to the server in one batch.
     * @param includeErrors also retry lines previously rejected (manual sync)
     */
    suspend fun syncPendingLines(includeErrors: Boolean = false): Result<BatchSyncResult> {
        return withContext(Dispatchers.IO) {
            try {
                val pending = if (includeErrors) {
                    lineDao.getUnsyncedLines()
                } else {
                    lineDao.getPendingSyncLines()
                }
                if (pending.isEmpty()) {
                    return@withContext Result.success(BatchSyncResult(0, 0, emptyList()))
                }

                val response = apiService.synchronizeInventoryLines(pending.map { it.toSyncPayload() })

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    val failedIds = result.failedIds.orEmpty().toSet()
                    val conflictedIds = result.conflictedIds.orEmpty().toSet()
                    pending.forEach { entity ->
                        when (entity.id) {
                            // Conflit : ne sera pas rejoué automatiquement (donnée périmée),
                            // la ligne est signalée à l'opérateur pour arbitrage
                            in conflictedIds -> lineDao.updateLine(
                                entity.copy(syncStatus = STATUS_CONFLICT)
                            )
                            in failedIds -> lineDao.updateLine(
                                entity.copy(syncStatus = STATUS_ERROR)
                            )
                            else -> lineDao.updateLine(
                                entity.copy(syncStatus = STATUS_SYNCED, locallyModified = false)
                            )
                        }
                    }
                    Log.d(
                        TAG,
                        "Batch sync: ${result.saved} saved, ${result.failed} failed " +
                            "(${result.conflicted} conflicts)"
                    )
                    Result.success(result)
                } else {
                    Result.failure(Exception("Failed to synchronize: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error synchronizing pending lines", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Search product by barcode (CIP/EAN) — backend returns a list, takes the first match
     */
    suspend fun searchProductByBarcode(barcode: String): Result<Product> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchProductByCode(barcode)

                if (response.isSuccessful && response.body() != null) {
                    val product = response.body()!!.firstOrNull()
                    if (product != null) {
                        Log.d(TAG, "Found product: ${product.libelle}")
                        Result.success(product)
                    } else {
                        Result.failure(Exception("Produit non trouvé"))
                    }
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
     * Get lots of an inventory line (network only)
     */
    suspend fun getInventoryLots(lineId: Long): Result<List<InventoryLot>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInventoryLots(lineId)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Loaded ${response.body()!!.size} lots for line $lineId")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load lots: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading lots", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Create a lot on an inventory line — the backend re-syncs the parent
     * line quantity (sum of lots)
     */
    suspend fun createInventoryLot(lineId: Long, lot: InventoryLot): Result<InventoryLot> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createInventoryLot(lineId, lot)

                if (response.isSuccessful && response.body() != null) {
                    val created = response.body()!!
                    Log.d(TAG, "Created lot ${created.numLot} on line $lineId")
                    Result.success(created)
                } else {
                    Result.failure(Exception("Failed to create lot: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating lot", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update a lot quantity — the backend re-syncs the parent line quantity
     */
    suspend fun updateInventoryLot(lot: InventoryLot): Result<InventoryLot> {
        val lotId = lot.id
            ?: return Result.failure(Exception("Lot sans identifiant"))
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateInventoryLot(lotId, lot)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Updated lot $lotId")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to update lot: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating lot", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete an inventory lot
     */
    suspend fun deleteInventoryLot(lotId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteInventoryLot(lotId)

                if (response.isSuccessful) {
                    Log.d(TAG, "Deleted lot $lotId")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete lot: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting lot", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Lines have a FK on store_inventories: make sure the parent row exists
     * before inserting lines (network fetch, or minimal stub when offline).
     */
    private suspend fun ensureInventoryCached(inventoryId: Long) {
        if (inventoryDao.getInventoryById(inventoryId) != null) return
        try {
            val response = apiService.getInventory(inventoryId)
            if (response.isSuccessful && response.body() != null) {
                inventoryDao.upsertInventory(InventoryEntity.fromModel(response.body()!!))
                return
            }
        } catch (_: Exception) {
            // offline — fall through to stub
        }
        inventoryDao.upsertInventory(
            InventoryEntity(
                id = inventoryId,
                categoryName = null,
                categoryLabel = null,
                statut = InventoryStatut.PROCESSING.name,
                inventoryType = null,
                description = null,
                createdAt = null,
                updatedAt = null,
                inventoryAmountBegin = 0,
                inventoryAmountAfter = 0,
                inventoryValueCostBegin = 0,
                inventoryValueCostAfter = 0,
                gapCost = 0,
                gapAmount = 0,
                storageId = null,
                storageName = null,
                rayonId = null,
                rayonLibelle = null,
                abbrName = null
            )
        )
    }
}
