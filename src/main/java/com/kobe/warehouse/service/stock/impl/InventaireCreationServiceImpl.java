package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.InventaireCreationService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventaireCreationServiceImpl implements InventaireCreationService {

    private static final Logger log = LoggerFactory.getLogger(InventaireCreationServiceImpl.class);

    private static final int DEFAULT_ALERTE_JOURS = 90;

    // ── Types de périmètre ────────────────────────────────────────────────────

    /**
     * MAGASIN : tous les produits actifs, une ligne par produit rattachée au storage PRINCIPAL.
     * Le pharmacien saisit le stock total (rayon + réserve consolidés).
     * Après clôture, proc_close_inventory_v2 STEP 1 met à jour le PRINCIPAL et STEP 2 remet
     * la réserve (SAFETY_STOCK) à 0 — cohérent avec l'approche de comptage consolidé.
     */
    private static final String SQL_INSERT_MAGASIN =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            WHERE p.status = 'ENABLE'
            """;

    /**
     * FAMILLY : tous les produits actifs d'une famille
     */
    private static final String SQL_INSERT_FAMILLY =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            WHERE p.status = 'ENABLE'
              AND p.famille_id = :famillyId
            """;

    /**
     * RAYON : tous les produits actifs d'un rayon
     */
    private static final String SQL_INSERT_RAYON =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            JOIN rayon_produit rp ON p.id = rp.produit_id
            JOIN rayon r           ON r.id = rp.rayon_id
            WHERE p.status = 'ENABLE'
              AND r.id = :rayonId
            """;

    /**
     * STORAGE : tous les produits actifs d'un storage (point de vente)
     */
    private static final String SQL_INSERT_STORAGE =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            JOIN rayon_produit rp ON p.id = rp.produit_id
            JOIN rayon r           ON r.id = rp.rayon_id
            JOIN storage s         ON s.id = r.storage_id
            WHERE p.status = 'ENABLE'
              AND s.id = :storageId
            """;

    // ── Types thématiques ─────────────────────────────────────────────────────

    /**
     * PERIME : produits ayant au moins un lot périmé avec du stock restant. Source : table `lot`
     * avec expiry_date < CURRENT_DATE et current_quantity > 0.
     */
    private static final String SQL_INSERT_PERIME =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT DISTINCT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            JOIN lot l ON l.produit_id = p.id
            WHERE p.status = 'ENABLE'
              AND l.expiry_date < CURRENT_DATE
              AND l.current_quantity > 0
            """;

    /**
     * PERIME — lots : un inventory_lot par lot périmé, rattaché à la store_inventory_line du
     * produit.
     */
    private static final String SQL_INSERT_LOTS_PERIME =
        """
            INSERT INTO inventory_lot (lot_id, quantity_init, store_inventory_line_id, updated_at, updated, gap)
            SELECT l.id, l.current_quantity, sil.id, NOW(), false, 0
            FROM store_inventory_line sil
            JOIN lot l ON l.produit_id = sil.produit_id
            WHERE sil.store_inventory_id = :inventoryId
              AND l.expiry_date < CURRENT_DATE
              AND l.current_quantity > 0
            ON CONFLICT ON CONSTRAINT uq_il_lot_line DO NOTHING
            """;

    /**
     * ALERTE_PEREMPTION : produits dont un lot expire dans les :alerteJours prochains. Paramètre :
     * alerteJours (défaut = 90 jours).
     */
    private static final String SQL_INSERT_ALERTE_PEREMPTION =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT DISTINCT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            JOIN lot l ON l.produit_id = p.id
            WHERE p.status = 'ENABLE'
              AND l.expiry_date BETWEEN CURRENT_DATE AND (CURRENT_DATE + CAST(:alerteJours AS INT))
              AND l.current_quantity > 0
            """;

    /**
     * ALERTE_PEREMPTION — lots : un inventory_lot par lot proche de péremption.
     */
    private static final String SQL_INSERT_LOTS_ALERTE_PEREMPTION =
        """
            INSERT INTO inventory_lot (lot_id, quantity_init, store_inventory_line_id, updated_at, updated, gap)
            SELECT l.id, l.current_quantity, sil.id, NOW(), false, 0
            FROM store_inventory_line sil
            JOIN lot l ON l.produit_id = sil.produit_id
            WHERE sil.store_inventory_id = :inventoryId
              AND l.expiry_date BETWEEN CURRENT_DATE AND (CURRENT_DATE + CAST(:alerteJours AS INT))
              AND l.current_quantity > 0
            ON CONFLICT ON CONSTRAINT uq_il_lot_line DO NOTHING
            """;

    /**
     * Générique : un inventory_lot par lot actif (current_quantity > 0), pour tous les types
     * d'inventaire quand GESTION_LOT_INVENTAIRE = true. Exécuté après l'insertion des
     * store_inventory_line.
     */
    private static final String SQL_INSERT_LOTS_GENERIC =
        """
            INSERT INTO inventory_lot (lot_id, quantity_init, store_inventory_line_id, updated_at, updated, gap)
            SELECT l.id, l.current_quantity, sil.id, NOW(), false, 0
            FROM store_inventory_line sil
            JOIN lot l ON l.produit_id = sil.produit_id
            WHERE sil.store_inventory_id = :inventoryId
              AND l.current_quantity > 0
            ON CONFLICT ON CONSTRAINT uq_il_lot_line DO NOTHING
            """;

    /**
     * VENDU : produits vendus au moins une fois sur la période [:dateFrom, :dateTo]. Source : table
     * `sales_line` (hors lignes ignorées et ventes annulées).
     */
    private static final String SQL_INSERT_VENDU =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT DISTINCT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            JOIN sales_line sl ON sl.produit_id = p.id
            JOIN sales s       ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
            WHERE p.status = 'ENABLE'
              AND sl.sale_date BETWEEN :dateFrom AND :dateTo
              AND s.statut = 'CLOSED'
              AND s.canceled  = false
            """;

    /**
     * INVENDU : produits actifs sans aucune vente sur la période [:dateFrom, :dateTo]. Utile pour
     * identifier le stock dormant et planifier le déstockage.
     */
    private static final String SQL_INSERT_INVENDU =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            WHERE p.status = 'ENABLE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM sales_line sl
                  JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
                  WHERE sl.produit_id = p.id
                    AND sl.sale_date BETWEEN :dateFrom AND :dateTo
                    AND s.statut = 'CLOSED'
                    AND s.canceled   = false
              )
            """;

    /**
     * SOUS_SEUIL : produits dont le stock du storage inventorié (:storageId) est inférieur ou égal
     * au seuil minimum défini sur ce même storage.
     *
     * <p>Critère : stock(storage inventorié) ≤ seuil_mini(storage inventorié).
     * On compare chaque storage à son propre seuil, sans agréger rayon + réserve.
     * Exemple : rayon = 3, seuil_mini = 5, réserve = 20 → produit inclus (rayon sous seuil)
     * même si le stock total dépasse le seuil. Le réassort rayon ← réserve sera suggéré
     * par l'InventoryClosedEventListener après clôture.
     */
    private static final String SQL_INSERT_SOUS_SEUIL =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            WHERE p.status = 'ENABLE'
              AND EXISTS (
                  SELECT 1
                  FROM stock_produit sp
                  WHERE sp.produit_id  = p.id
                    AND sp.storage_id  = :storageId
                    AND sp.seuil_mini IS NOT NULL
                    AND (sp.qty_stock + sp.qty_ug) <= sp.seuil_mini
              )
            """;

    /**
     * SELECTION_PRODUIT : insertion directe à partir d'une liste explicite d'IDs de produits. Seuls
     * les produits actifs (status = 'ENABLE') de la liste sont retenus.
     */
    private static final String SQL_INSERT_SELECTION_PRODUIT =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            WHERE p.status = 'ENABLE'
              AND p.id IN (:produitIds)
            """;

    /**
     * EN_RUPTURE : produits dont le stock TOTAL du magasin (rayon + réserve) est 0. Agrégation par
     * magasin : un produit n'est en rupture que si rayon ET réserve sont vides.
     */
    private static final String SQL_INSERT_EN_RUPTURE =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            WHERE p.status = 'ENABLE'
              AND EXISTS (
                  SELECT 1
                  FROM stock_produit sp
                  JOIN storage s ON s.id = sp.storage_id
                  WHERE sp.produit_id = p.id
                    AND s.magasin_id  = :magasinId
                  GROUP BY sp.produit_id
                  HAVING SUM(sp.qty_stock + sp.qty_ug) = 0
              )
            """;

    // ─────────────────────────────────────────────────────────────────────────

    private final StoreInventoryRepository storeInventoryRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final RayonRepository rayonRepository;
    private final EntityManager em;
    private final AppConfigurationService appConfigurationService;

    public InventaireCreationServiceImpl(
        StoreInventoryRepository storeInventoryRepository,
        UserService userService,
        UserRepository userRepository,
        StorageService storageService,
        RayonRepository rayonRepository,
        EntityManager em,
        AppConfigurationService appConfigurationService
    ) {
        this.storeInventoryRepository = storeInventoryRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.rayonRepository = rayonRepository;
        this.em = em;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public StoreInventoryDTO create(StoreInventoryRecord record) {
        StoreInventory inventory = buildInventory(record);
        inventory = storeInventoryRepository.saveAndFlush(inventory);
        insertLines(inventory, record);
        log.info("Inventaire créé : id={}, catégorie={}", inventory.getId(),
            inventory.getInventoryCategory());
        return new StoreInventoryDTO(inventory);
    }

    // ── Construction de l'entité ─────────────────────────────────────────────

    private StoreInventory buildInventory(StoreInventoryRecord record) {
        StoreInventory inventory = new StoreInventory();
        inventory.createdAt(LocalDateTime.now());
        inventory.updatedAt(inventory.getCreatedAt());
        // userId explicite (scheduler) ou utilisateur connecté (API)
        if (record.userId() != null) {
            inventory.setUser(userRepository.getReferenceById(record.userId()));
            inventory.setInventoryType(InventoryType.PROGRAMME);
        } else {
            inventory.setUser(userService.getUser());
        }
        inventory.setInventoryCategory(InventoryCategory.valueOf(record.inventoryCategory()));
        inventory.setInventoryAmountAfter(0L);
        inventory.setInventoryAmountBegin(0L);
        inventory.setInventoryValueCostBegin(0L);
        inventory.setInventoryValueCostAfter(0L);
        inventory.setDescription(record.description());
        inventory.setGapAmount(0);
        inventory.setGapCost(0);

        // Résolution du storage par défaut si non spécifié
        if (Objects.isNull(record.storage())) {
            inventory.setStorage(storageService.getDefaultConnectedUserMainStorage());
        }

        // Pour RAYON : le storage est déduit du rayon
        if (inventory.getInventoryCategory() == InventoryCategory.RAYON) {
            Rayon rayon = rayonRepository.getReferenceById(record.rayon());
            inventory.setRayon(rayon);
            inventory.setStorage(rayon.getStorage());
        }

        // Cas STORAGE ou MAGASIN avec storage explicite
        if (Objects.isNull(inventory.getStorage())) {
            inventory.setStorage(storageService.getOne(record.storage()));
        }

        return inventory;
    }

    // ── Insertion des lignes avec paramètres nommés ──────────────────────────

    private void insertLines(StoreInventory inventory, StoreInventoryRecord record) {
        Long inventoryId = inventory.getId();
        Integer storageId = inventory.getStorage().getId();
        Integer magasinId = inventory.getStorage().getMagasin().getId();
        boolean gestionLot = appConfigurationService.useGestionLotInventaire();

        switch (inventory.getInventoryCategory()) {

            // ── Types de périmètre ────────────────────────────────────────────
            case MAGASIN -> {
                em.createNativeQuery(SQL_INSERT_MAGASIN)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case FAMILLY -> {
                em.createNativeQuery(SQL_INSERT_FAMILLY)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .setParameter("famillyId", record.famillyId())
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case RAYON -> {
                em.createNativeQuery(SQL_INSERT_RAYON)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .setParameter("rayonId", inventory.getRayon().getId())
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case STORAGE -> {
                em.createNativeQuery(SQL_INSERT_STORAGE)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            // ── Types thématiques ─────────────────────────────────────────────
            // PERIME et ALERTE_PEREMPTION créent toujours leurs lots (filtrés par date)
            case PERIME -> {
                em.createNativeQuery(SQL_INSERT_PERIME)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .executeUpdate();
                em.flush();
                em.createNativeQuery(SQL_INSERT_LOTS_PERIME)
                    .setParameter("inventoryId", inventoryId)
                    .executeUpdate();
            }

            case ALERTE_PEREMPTION -> {
                int alerteJours = Objects.requireNonNullElse(record.alerteJours(),
                    DEFAULT_ALERTE_JOURS);
                em.createNativeQuery(SQL_INSERT_ALERTE_PEREMPTION)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .setParameter("alerteJours", alerteJours)
                    .executeUpdate();
                em.flush();
                em.createNativeQuery(SQL_INSERT_LOTS_ALERTE_PEREMPTION)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("alerteJours", alerteJours)
                    .executeUpdate();
            }

            case VENDU -> {
                requireDateRange(record, InventoryCategory.VENDU);
                em.createNativeQuery(SQL_INSERT_VENDU)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .setParameter("dateFrom", record.dateFrom())
                    .setParameter("dateTo", record.dateTo())
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case INVENDU -> {
                requireDateRange(record, InventoryCategory.INVENDU);
                em.createNativeQuery(SQL_INSERT_INVENDU)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .setParameter("dateFrom", record.dateFrom())
                    .setParameter("dateTo", record.dateTo())
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case SOUS_SEUIL -> {
                em.createNativeQuery(SQL_INSERT_SOUS_SEUIL)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case EN_RUPTURE -> {
                em.createNativeQuery(SQL_INSERT_EN_RUPTURE)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .setParameter("magasinId", magasinId)
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case ABC -> {
                em.createNativeQuery(StoreInventoryLineFilterBuilder.SQL_INSERT_ABC)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .setParameter("classePareto", record.classePareto())
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case SELECTION_PRODUIT -> {
                List<Integer> ids = record.produitIds();
                if (ids == null || ids.isEmpty()) {
                    throw new IllegalArgumentException(
                        "produitIds est obligatoire pour le type SELECTION_PRODUIT");
                }
                em.createNativeQuery(SQL_INSERT_SELECTION_PRODUIT)
                    .setParameter("inventoryId", inventoryId)
                    .setParameter("storageId", storageId)
                    .setParameter("produitIds", ids)
                    .executeUpdate();
                if (gestionLot) {
                    insertLotsGeneric(inventoryId);
                }
            }

            case GROSSISTE -> throw new IllegalArgumentException("Not supported yet.");
        }
    }

    private void insertLotsGeneric(Long inventoryId) {
        em.flush();
        em.createNativeQuery(SQL_INSERT_LOTS_GENERIC)
            .setParameter("inventoryId", inventoryId)
            .executeUpdate();
    }

    private void requireDateRange(StoreInventoryRecord record, InventoryCategory category) {
        if (record.dateFrom() == null || record.dateTo() == null) {
            throw new IllegalArgumentException(
                "Les paramètres dateFrom et dateTo sont obligatoires pour le type "
                    + category.name()
            );
        }
        if (record.dateTo().isBefore(record.dateFrom())) {
            throw new IllegalArgumentException(
                "dateTo (" + record.dateTo() + ") doit être >= dateFrom (" + record.dateFrom() + ")"
            );
        }
    }
}
