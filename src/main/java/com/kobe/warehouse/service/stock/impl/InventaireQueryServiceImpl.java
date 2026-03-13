package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.enumeration.StoreInventoryLineEnum;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.stock.InventaireQueryService;
import com.kobe.warehouse.service.stock.InventoryStockService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class InventaireQueryServiceImpl implements InventaireQueryService {

    private final Logger log = LoggerFactory.getLogger(InventaireQueryServiceImpl.class);

    private final StoreInventoryRepository storeInventoryRepository;
    private final InventoryStockService inventoryStockService;
    private final EntityManager em;

    public InventaireQueryServiceImpl(
        StoreInventoryRepository storeInventoryRepository,
        InventoryStockService inventoryStockService,
        EntityManager em
    ) {
        this.storeInventoryRepository = storeInventoryRepository;
        this.inventoryStockService = inventoryStockService;
        this.em = em;
    }

    @Override
    public Page<StoreInventoryLineRecord> getInventoryPage(
        StoreInventoryLineFilterRecord filter,
        Pageable pageable,
        boolean excludeIfClosed
    ) {
        StoreInventory inventory = storeInventoryRepository.getReferenceById(
            filter.storeInventoryId());

        if (excludeIfClosed && inventory.getStatut() == InventoryStatut.CLOSED) {
            return Page.empty(pageable);
        }

        long count = countItems(inventory, filter);
        if (count == 0) {
            return Page.empty(pageable);
        }

        List<Tuple> tuples = fetchTuples(filter, pageable);

        // Pré-chargement du stock en masse : 1 requête pour toute la page (corrige le N+1)
        Set<Integer> produitIds = tuples.stream()
            .map(t -> t.get("produitId", Integer.class))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Integer, Integer> stockMap = buildStockMap(inventory, produitIds);

        List<StoreInventoryLineRecord> records = tuples.stream()
            .filter(t -> t.get("produitId", Integer.class) != null)
            .map(t -> toRecord(t, stockMap))
            .toList();

        return new PageImpl<>(records, pageable, count);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Route vers buildStockMapByStorage ou buildStockMapByMagasin selon le type d'inventaire.
     * <p>
     * - RAYON / STORAGE → stock du storage précis uniquement - MAGASIN + types thématiques →
     * agrégation rayon + réserve par magasin
     */
    private Map<Integer, Integer> buildStockMap(StoreInventory inventory, Set<Integer> produitIds) {
        if (inventory.getStorage() == null || produitIds.isEmpty()) {
            return Map.of();
        }
        if (usesStorageScope(inventory.getInventoryCategory())) {
            return inventoryStockService.buildStockMapByStorage(
                inventory.getStorage().getId(), produitIds);
        }
        // MAGASIN et tous les types thématiques : agrégation rayon + réserve
        Integer magasinId = inventory.getStorage().getMagasin().getId();
        return inventoryStockService.buildStockMapByMagasin(magasinId, produitIds);
    }

    /**
     * Retourne true si l'inventaire est scopé à un storage précis (le stock ne doit PAS agréger la
     * réserve).
     */
    private boolean usesStorageScope(InventoryCategory category) {
        return category == InventoryCategory.RAYON || category == InventoryCategory.STORAGE;
    }

    private long countItems(StoreInventory inventory, StoreInventoryLineFilterRecord filter) {
        try {
            String countSql = buildQuery(StoreInventoryLineFilterBuilder.COUNT, filter);
            Object result = em.createNativeQuery(countSql)
                .setParameter(1, inventory.getId())
                .getSingleResult();
            return result instanceof Number n ? n.longValue() : 0L;
        } catch (Exception e) {
            log.error("Erreur comptage lignes inventaire id={}", inventory.getId(), e);
            return 0L;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> fetchTuples(StoreInventoryLineFilterRecord filter, Pageable pageable) {
        try {
            String sql = buildQuery(StoreInventoryLineFilterBuilder.BASE_QUERY, filter);
            return em.createNativeQuery(sql, Tuple.class)
                .setParameter(1, filter.storeInventoryId())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        } catch (Exception e) {
            log.error("Erreur récupération lignes inventaire id={}", filter.storeInventoryId(), e);
            return Collections.emptyList();
        }
    }

    private StoreInventoryLineRecord toRecord(Tuple t, Map<Integer, Integer> stockMap) {
        Integer produitId = t.get("produitId", Integer.class);
        int currentStock = stockMap.getOrDefault(produitId, 0);
        return new StoreInventoryLineRecord(
            produitId,
            t.get("code_cip", String.class),
            t.get("code_ean_labo", String.class),
            t.get("libelle", String.class),
            t.get("id", Long.class),
            t.get("gap", Integer.class),
            t.get("quantity_on_hand", Integer.class),
            currentStock,
            t.get("updated", Boolean.class),
            t.get("prix_achat", Integer.class),
            t.get("prix_uni", Integer.class)
        );
    }

    // ── Construction des requêtes SQL (réplication des default methods de InventaireService) ────

    private String buildQuery(String baseQuery, StoreInventoryLineFilterRecord filter) {
        String query = applyJoins(baseQuery, filter);
        String filterClause = buildFilterClause(filter.selectedFilter());
        if (StringUtils.hasLength(filter.search())) {
            return String.format(query, buildSearchClause(filter) + filterClause);
        } else if (StringUtils.hasLength(filterClause)) {
            return String.format(query, filterClause);
        }
        return String.format(query, " ");
    }

    private String applyJoins(String baseQuery, StoreInventoryLineFilterRecord filter) {
        if (filter.rayonId() != null) {
            return baseQuery
                .replace("{join_statement}", StoreInventoryLineFilterBuilder.RAYON_STATEMENT)
                .replace("{join_statement_where}",
                    String.format(StoreInventoryLineFilterBuilder.RAYON_STATEMENT_WHERE,
                        filter.rayonId()));
        }
        if (filter.storageId() != null) {
            return baseQuery
                .replace("{join_statement}", StoreInventoryLineFilterBuilder.RAYON_STATEMENT)
                .replace("{join_statement_where}",
                    String.format(StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT_WHERE,
                        filter.storageId()));
        }
        return baseQuery
            .replace("{join_statement}", "")
            .replace("{join_statement_where}", "");
    }

    private String buildSearchClause(StoreInventoryLineFilterRecord filter) {
        String term = filter.search() + "%";
        return String.format(StoreInventoryLineFilterBuilder.LIKE_STATEMENT_WHERE, term, term,
            term);
    }

    private String buildFilterClause(StoreInventoryLineEnum lineEnum) {
        if (lineEnum == null || lineEnum == StoreInventoryLineEnum.NONE) {
            return "";
        }
        return switch (lineEnum) {
            case NOT_UPDATED -> " AND a.updated IS false ";
            case UPDATED -> " AND a.updated ";
            case GAP -> " AND a.updated  AND  a.quantity_on_hand <> a.quantity_init ";
            case GAP_NEGATIF -> " AND a.updated  AND  a.quantity_on_hand < a.quantity_init ";
            case GAP_POSITIF -> " AND a.updated  AND  a.quantity_on_hand >= a.quantity_init ";
            default -> "";
        };
    }
}
