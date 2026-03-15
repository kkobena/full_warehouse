package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.dto.InventoryExportSummary;
import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLineExport;
import com.kobe.warehouse.service.dto.StoreInventoryLotGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLotLineExport;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.enumeration.InventoryExportSummaryEnum;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.InventaireQueryService;
import com.kobe.warehouse.service.stock.InventoryStockService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventaireQueryServiceImpl implements InventaireQueryService {

    private final Logger log = LoggerFactory.getLogger(InventaireQueryServiceImpl.class);

    private final StoreInventoryRepository storeInventoryRepository;
    private final InventoryStockService inventoryStockService;
    private final AppConfigurationService appConfigurationService;
    private final EntityManager em;

    public InventaireQueryServiceImpl(
        StoreInventoryRepository storeInventoryRepository,
        InventoryStockService inventoryStockService,
        AppConfigurationService appConfigurationService,
        EntityManager em
    ) {
        this.storeInventoryRepository = storeInventoryRepository;
        this.inventoryStockService = inventoryStockService;
        this.appConfigurationService = appConfigurationService;
        this.em = em;
    }

    private static int toInt(Object val) {
        return val instanceof Number n ? n.intValue() : 0;
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

        long count = countItems(filter);
        if (count == 0) {
            return Page.empty(pageable);
        }

        InventoryCategory category = inventory.getInventoryCategory();
        boolean gestionLot = appConfigurationService.useGestionLotInventaire();
        boolean isAbc = category == InventoryCategory.ABC;

        List<Tuple> tuples = fetchTuples(filter, pageable, gestionLot, isAbc);

        // Pré-chargement du stock en masse : 1 requête pour toute la page (évite le N+1)
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

    @Override
    public InventoryExportWrapper exportInventory(
        StoreInventoryExportRecord inventoryExportRecord) {
        List<StoreInventoryGroupExport> items = getStoreInventoryToExport(inventoryExportRecord);
        if (items.isEmpty()) {
            return null;
        }
        InventoryExportWrapper wrapper = new InventoryExportWrapper();
        wrapper.setInventoryGroups(items);
        wrapper.setInventoryExportSummaries(buildSummaries(items));
        wrapper.setStoreInventory(
            new StoreInventoryDTO(storeInventoryRepository.getReferenceById(
                inventoryExportRecord.filterRecord().storeInventoryId()))
        );
        wrapper.setExportGroupBy(inventoryExportRecord.exportGroupBy());
        return wrapper;
    }

    @Override
    public List<StoreInventoryGroupExport> getStoreInventoryToExport(
        StoreInventoryExportRecord filterRecord) {
        return buildStoreInventoryGroupExportsFromTuple(fetchExportTuples(filterRecord),
            filterRecord);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<StoreInventoryLotGroupExport> getLotGroupsForExport(Long inventoryId) {
        List<Tuple> tuples = em.createNativeQuery(
                StoreInventoryLineFilterBuilder.LOT_EXPORT_SQL,
                Tuple.class)
            .setParameter(1, inventoryId)
            .getResultList();

        LinkedHashMap<String, StoreInventoryLotGroupExport> map = new LinkedHashMap<>();
        for (Tuple t : tuples) {
            String cip = t.get("code_cip", String.class);
            StoreInventoryLotGroupExport group = map.computeIfAbsent(cip, k ->
                new StoreInventoryLotGroupExport(cip, t.get("produit_libelle", String.class))
            );
            group.addLot(new StoreInventoryLotLineExport(
                cip,
                t.get("produit_libelle", String.class),
                t.get("num_lot", String.class),
                t.get("expiry_date", java.time.LocalDate.class),
                toInt(t.get("quantity_init")),
                toInt(t.get("quantity_on_hand")),
                toInt(t.get("gap")),
                toInt(t.get("last_unit_price")),
                toInt(t.get("prix_achat"))
            ));
        }
        return new ArrayList<>(map.values());
    }

    private long countItems(StoreInventoryLineFilterRecord filter) {
        try {
            String sql = StoreInventoryLineFilterBuilder.lineQuery(filter).buildCount();
            Object result = em.createNativeQuery(sql)
                .setParameter(1, filter.storeInventoryId())
                .getSingleResult();
            return result instanceof Number n ? n.longValue() : 0L;
        } catch (Exception e) {
            log.error("Erreur comptage lignes inventaire id={}", filter.storeInventoryId(), e);
            return 0L;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> fetchTuples(
        StoreInventoryLineFilterRecord filter,
        Pageable pageable,
        boolean gestionLot,
        boolean isAbc
    ) {
        try {
            String sql = StoreInventoryLineFilterBuilder.lineQuery(filter)
                .withLotCount(gestionLot)
                .withAbcPareto(isAbc)
                .buildPage();
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

    @SuppressWarnings("unchecked")
    private List<Tuple> fetchExportTuples(StoreInventoryExportRecord record) {
        try {
            return em.createNativeQuery(
                    StoreInventoryLineFilterBuilder.buildExportQuery(record), Tuple.class)
                .setParameter(1, record.filterRecord().storeInventoryId())
                .getResultList();
        } catch (Exception e) {
            log.error("Erreur export inventaire id={}", record.filterRecord().storeInventoryId(),
                e);
            return Collections.emptyList();
        }
    }

    private StoreInventoryLineRecord toRecord(Tuple t, Map<Integer, Integer> stockMap) {
        Integer produitId = t.get("produitId", Integer.class);
        Integer lotCount = t.get("lot_count", Integer.class);
        return new StoreInventoryLineRecord(
            produitId,
            t.get("code_cip", String.class),
            t.get("code_ean_labo", String.class),
            t.get("libelle", String.class),
            t.get("id", Long.class),
            t.get("gap", Integer.class),
            t.get("quantity_on_hand", Integer.class),
            stockMap.getOrDefault(produitId, 0),
            t.get("updated", Boolean.class),
            t.get("prix_achat", Integer.class),
            t.get("prix_uni", Integer.class),
            t.get("storage_id", Integer.class),
            t.get("seuil_mini", Integer.class),
            lotCount != null ? lotCount : 0,
            t.get("classe_pareto", String.class)
        );
    }

    /**
     * RAYON / STORAGE → stock du storage précis uniquement. Autres types → agrégation rayon +
     * réserve par magasin.
     */
    private Map<Integer, Integer> buildStockMap(StoreInventory inventory, Set<Integer> produitIds) {
        if (inventory.getStorage() == null || produitIds.isEmpty()) {
            return Map.of();
        }
        InventoryCategory category = inventory.getInventoryCategory();
        if (category == InventoryCategory.RAYON || category == InventoryCategory.STORAGE) {
            return inventoryStockService.buildStockMapByStorage(
                inventory.getStorage().getId(), produitIds);
        }
        return inventoryStockService.buildStockMapByMagasin(
            inventory.getStorage().getMagasin().getId(), produitIds);
    }

    private List<StoreInventoryGroupExport> buildStoreInventoryGroupExportsFromTuple(
        List<Tuple> tuples,
        StoreInventoryExportRecord record
    ) {
        LinkedHashMap<Long, StoreInventoryGroupExport> map = new LinkedHashMap<>();
        switch (record.exportGroupBy()) {
            case FAMILLY -> {
                for (Tuple t : tuples) {
                    Number raw = (Number) t.get("famillyId");
                    long id = raw != null ? raw.longValue() : -1L;
                    StoreInventoryLineExport line = StoreInventoryLineFilterBuilder.buildStoreInventoryLineExportRecord(t);
                    map.put(id, buildFromTuple(map.get(id), id, line.getFamillyCode(), line.getFamillyLibelle(), line));
                }
            }
            case RAYON, NONE -> {
                for (Tuple t : tuples) {
                    Number rawRayon = (Number) t.get("rayon_id");
                    Long idRayon = rawRayon != null ? rawRayon.longValue() : null;
                    StoreInventoryLineExport line = StoreInventoryLineFilterBuilder.buildStoreInventoryLineExportRecord(t);
                    Triple<Long, String, String> triple = getRayon(idRayon, line);
                    long id = triple.getLeft();
                    map.put(id, buildFromTuple(map.get(id), id, triple.getMiddle(), triple.getRight(), line));
                }
            }
            case STORAGE -> {
                for (Tuple t : tuples) {
                    Number rawStorage = (Number) t.get("storage_id");
                    long id = rawStorage != null ? rawStorage.longValue() : -1L;
                    StoreInventoryLineExport line = StoreInventoryLineFilterBuilder.buildStoreInventoryLineExportRecord(t);
                    map.put(id, buildFromTuple(map.get(id), id, line.getStorageLibelle(), line.getStorageLibelle(), line));
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    private StoreInventoryGroupExport buildFromTuple(
        StoreInventoryGroupExport group,
        long id,
        String code,
        String libelle,
        StoreInventoryLineExport line
    ) {
        if (Objects.isNull(group)) {
            group = new StoreInventoryGroupExport();
            group.setId(id);
            group.setCode(code);
            group.setLibelle(libelle);
        }
        group.getItems().add(line);
        group.computeSummary(line);
        return group;
    }

    private Triple<Long, String, String> getRayon(Long idRayon, StoreInventoryLineExport line) {
        if (Objects.isNull(idRayon)) {
            return Triple.of(-1L, "SANS", "RAYON");
        }
        long i = idRayon;
        return Stream.of(1L, 2L, 3L).anyMatch(e -> e == i)
            ? Triple.of(-1L, "SANS", "RAYON")
            : Triple.of(i, line.getRayonCode(), line.getRayonLibelle());
    }

    private Map<String, InventoryExportSummary> buildSummaries(
        List<StoreInventoryGroupExport> datas) {
        InventoryExportSummary achatAvant = new InventoryExportSummary();
        achatAvant.setName(InventoryExportSummaryEnum.ACHAT_AVANT);
        InventoryExportSummary achatApres = new InventoryExportSummary();
        achatApres.setName(InventoryExportSummaryEnum.ACHAT_APRES);
        InventoryExportSummary venteAvant = new InventoryExportSummary();
        venteAvant.setName(InventoryExportSummaryEnum.VENTE_AVANT);
        InventoryExportSummary venteApres = new InventoryExportSummary();
        venteApres.setName(InventoryExportSummaryEnum.VENTE_APRES);
        InventoryExportSummary achatEcart = new InventoryExportSummary();
        achatEcart.setName(InventoryExportSummaryEnum.ACHAT_ECART);
        InventoryExportSummary venteEcart = new InventoryExportSummary();
        venteEcart.setName(InventoryExportSummaryEnum.VENTE_ECART);

        for (StoreInventoryGroupExport export : datas) {
            List<InventoryExportSummary> totaux = Stream.of(
                    export.getTotaux(), export.getTotauxEcart(), export.getTotauxVente())
                .flatMap(List::stream)
                .toList();
            for (InventoryExportSummary s : totaux) {
                switch (s.getName()) {
                    case ACHAT_AVANT -> achatAvant.setValue(achatAvant.getValue() + s.getValue());
                    case ACHAT_APRES -> achatApres.setValue(achatApres.getValue() + s.getValue());
                    case ACHAT_ECART -> achatEcart.setValue(achatEcart.getValue() + s.getValue());
                    case VENTE_APRES -> venteApres.setValue(venteApres.getValue() + s.getValue());
                    case VENTE_AVANT -> venteAvant.setValue(venteAvant.getValue() + s.getValue());
                    case VENTE_ECART -> venteEcart.setValue(venteEcart.getValue() + s.getValue());
                }
            }
        }
        return Map.of(
            "achatAvant", achatAvant,
            "achatApres", achatApres,
            "venteAvant", venteAvant,
            "venteApres", venteApres,
            "achatEcart", achatEcart,
            "venteEcart", venteEcart
        );
    }
}
