package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Rayon_;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage_;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.StoreInventory_;
import com.kobe.warehouse.domain.User_;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.InventaireService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.dto.InventoryExportSummary;
import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLineExport;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.enumeration.InventoryExportSummaryEnum;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryFilterRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import com.kobe.warehouse.service.errors.InventoryException;
import com.kobe.warehouse.service.report.InventoryReportReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventaireServiceImpl implements InventaireService {

    private final Logger log = LoggerFactory.getLogger(InventaireServiceImpl.class);

    private final UserService userService;

    private final StoreInventoryRepository storeInventoryRepository;

    private final StoreInventoryLineRepository storeInventoryLineRepository;
    private final StorageService storageService;
    private final StockProduitRepository stockProduitRepository;
    private final RayonRepository rayonRepository;
    private final InventoryReportReportService inventoryReportService;
    private final WarehouseCalendarService warehouseCalendarService;

    @PersistenceContext
    private EntityManager em;

    public InventaireServiceImpl(
        UserService userService,
        StoreInventoryRepository storeInventoryRepository,
        StoreInventoryLineRepository storeInventoryLineRepository,
        StorageService storageService,
        StockProduitRepository stockProduitRepository,
        RayonRepository rayonRepository,
        InventoryReportReportService inventoryReportService,
        WarehouseCalendarService warehouseCalendarService
    ) {
        this.userService = userService;
        this.storeInventoryRepository = storeInventoryRepository;
        this.storeInventoryLineRepository = storeInventoryLineRepository;
        this.storageService = storageService;
        this.stockProduitRepository = stockProduitRepository;
        this.rayonRepository = rayonRepository;
        this.inventoryReportService = inventoryReportService;
        this.warehouseCalendarService = warehouseCalendarService;
    }

    @Override
    public Resource printToPdf(StoreInventoryExportRecord filterRecord) throws MalformedURLException {
        return this.inventoryReportService.printToPdf(this.exportInventory(filterRecord));
    }

    @Override
    public ItemsCountRecord close(Long id) throws InventoryException {
        long count = storeInventoryLineRepository.countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(id);
        if (count > 0) {
            throw new InventoryException();
        }
        StoreInventory storeInventory = storeInventoryRepository.getReferenceById(id);
        storeInventory.setStatut(InventoryStatut.CLOSED);
        storeInventory.setUpdatedAt(LocalDateTime.now());
        StoreInventorySummaryRecord storeInventorySummaryRecord = fetchSummary(id);
        storeInventory.setInventoryAmountBegin(storeInventorySummaryRecord.amountValueBegin().longValue());
        storeInventory.setInventoryValueCostAfter(storeInventorySummaryRecord.costValueAfter().longValue());
        storeInventory.setInventoryAmountAfter(storeInventorySummaryRecord.amountValueAfter().longValue());
        storeInventory.setInventoryValueCostBegin(storeInventorySummaryRecord.costValueBegin().longValue());
        storeInventory.setGapCost(storeInventorySummaryRecord.gapCost().intValue());
        storeInventory.setGapAmount(storeInventorySummaryRecord.gapAmount().intValue());
        int itemCount = closeItems(storeInventory.getId());
        storeInventoryRepository.save(storeInventory);
        return new ItemsCountRecord(itemCount);
    }

    @Override
    public List<StoreInventoryGroupExport> getStoreInventoryToExport(StoreInventoryExportRecord filterRecord) {
        return buildStoreInventoryGroupExportsFromTuple(getAllByInventories(filterRecord), filterRecord);
    }

    private int closeItems(Long id) {
        return this.storeInventoryLineRepository.procCloseInventory(id.intValue());
    }

    private StoreInventorySummaryRecord fetchSummary(Long id) {
        return StoreInventoryLineFilterBuilder.buildSammary(
            (Tuple) this.em.createNativeQuery(StoreInventoryLineFilterBuilder.SUMMARY_SQL, Tuple.class)
                .setParameter(1, id)
                .getSingleResult()
        );
    }

    private void updateStoreInventoryLine(StoreInventoryLineDTO storeInventoryLineDTO, StoreInventoryLine storeInventoryLine) {
        Produit produit = storeInventoryLine.getProduit();
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        storeInventoryLine.setQuantitySold(0);
        storeInventoryLine.setUpdated(true);
        storeInventoryLine.setUpdatedAt(LocalDateTime.now());
        storeInventoryLine.setInventoryValueCost(
            Objects.nonNull(fournisseurProduit) ? fournisseurProduit.getPrixAchat() : produit.getCostAmount()
        );
        storeInventoryLine.setLastUnitPrice(
            Objects.nonNull(fournisseurProduit) ? fournisseurProduit.getPrixUni() : produit.getRegularUnitPrice()
        );
        storeInventoryLine.setQuantityOnHand(storeInventoryLineDTO.getQuantityOnHand());
        storeInventoryLine.setQuantityInit(storeInventoryLineDTO.getQuantityInit());
        storeInventoryLine.setGap(storeInventoryLine.getQuantityOnHand() - storeInventoryLine.getQuantityInit());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<StoreInventoryLineRecord> getAllByInventory(
        StoreInventoryLineFilterRecord storeInventoryLineFilterRecord,
        Pageable pageable
    ) {
        StoreInventory storeInventory = this.storeInventoryRepository.getReferenceById(storeInventoryLineFilterRecord.storeInventoryId());
        if (storeInventory.getStatut() == InventoryStatut.CLOSED) {
            return Page.empty(pageable);
        }

        long count = getTotalCount(storeInventory, storeInventoryLineFilterRecord);
        if (count == 0) {
            return Page.empty(pageable);
        }
        return new PageImpl<>(buildItems(storeInventory, storeInventoryLineFilterRecord, pageable), pageable, count);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<StoreInventoryLineRecord> getInventoryItems(
        StoreInventoryLineFilterRecord storeInventoryLineFilterRecord,
        Pageable pageable
    ) {
        StoreInventory storeInventory = this.storeInventoryRepository.getReferenceById(storeInventoryLineFilterRecord.storeInventoryId());
        long count = getTotalCount(storeInventory, storeInventoryLineFilterRecord);
        if (count == 0) {
            return Page.empty(pageable);
        }
        return new PageImpl<>(buildItems(storeInventory, storeInventoryLineFilterRecord, pageable), pageable, count);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<StoreInventoryDTO> storeInventoryList(StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable) {
        return new PageImpl<>(
            this.fetchInventories(storeInventoryFilterRecord, pageable).stream().map(StoreInventoryDTO::new).toList(),
            pageable,
            this.fetchInventoriesCount(storeInventoryFilterRecord)
        );
    }

    @Override
    public Optional<StoreInventoryDTO> getProccessingStoreInventory(Long id) {
        StoreInventory storeInventory = this.storeInventoryRepository.getReferenceById(id);
        if (storeInventory.getStatut() == InventoryStatut.CLOSED) {
            return Optional.empty();
        }
        return Optional.of(new StoreInventoryDTO(storeInventory));
    }

    @Override
    public InventoryExportWrapper exportInventory(StoreInventoryExportRecord inventoryExportRecord) {
        var items = this.getStoreInventoryToExport(inventoryExportRecord);
        if (items.isEmpty()) {
            return null;
        }
        InventoryExportWrapper inventoryExportWrapper = new InventoryExportWrapper();
        inventoryExportWrapper.setInventoryGroups(items);
        inventoryExportWrapper.setInventoryExportSummaries(buildSummaries(items));
        inventoryExportWrapper.setStoreInventory(
            new StoreInventoryDTO(this.storeInventoryRepository.getReferenceById(inventoryExportRecord.filterRecord().storeInventoryId()))
        );
        return inventoryExportWrapper;
    }

    private Map<String, InventoryExportSummary> buildSummaries(List<StoreInventoryGroupExport> datas) {
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
            List<InventoryExportSummary> totaux = Stream.of(export.getTotaux(), export.getTotauxEcart(), export.getTotauxVente())
                .flatMap(List::stream)
                .toList();

            for (InventoryExportSummary exportSummary : totaux) {
                switch (exportSummary.getName()) {
                    case ACHAT_AVANT -> updateSummary(achatAvant, exportSummary);
                    case ACHAT_APRES -> updateSummary(achatApres, exportSummary);
                    case ACHAT_ECART -> updateSummary(achatEcart, exportSummary);
                    case VENTE_APRES -> updateSummary(venteApres, exportSummary);
                    case VENTE_AVANT -> updateSummary(venteAvant, exportSummary);
                    case VENTE_ECART -> updateSummary(venteEcart, exportSummary);
                }
            }
        }
        return Map.of(
            "achatAvant",
            achatAvant,
            "achatApres",
            achatApres,
            "venteAvant",
            venteAvant,
            "venteApres",
            venteApres,
            "achatEcart",
            achatEcart,
            "venteEcart",
            venteEcart
        );
    }

    private void updateSummary(InventoryExportSummary inventoryExportSummary, InventoryExportSummary exportSummary) {
        inventoryExportSummary.setValue(inventoryExportSummary.getValue() + exportSummary.getValue());
    }

    private StoreInventoryGroupExport buildFromTuple(
        StoreInventoryGroupExport storeInventoryGroupExport,
        long id,
        String code,
        String libelle,
        StoreInventoryLineExport storeInventoryLineExport
    ) {
        if (Objects.isNull(storeInventoryGroupExport)) {
            storeInventoryGroupExport = new StoreInventoryGroupExport();
            storeInventoryGroupExport.setId(id);
            storeInventoryGroupExport.setCode(code);
            storeInventoryGroupExport.setLibelle(libelle);
        }

        storeInventoryGroupExport.getItems().add(storeInventoryLineExport);
        storeInventoryGroupExport.computeSummary(storeInventoryLineExport);
        return storeInventoryGroupExport;
    }

    private Triple<Long, String, String> getRayon(Long idRayon, StoreInventoryLineExport storeInventoryLineExport) {
        if (Objects.isNull(idRayon)) {
            return Triple.of(-1L, "SANS", "RAYON");
        }
        long i = idRayon;
        return Stream.of(1L, 2L, 3L).anyMatch(e -> e == i)
            ? Triple.of(-1L, "SANS", "RAYON")
            : Triple.of(i, storeInventoryLineExport.getRayonCode(), storeInventoryLineExport.getRayonLibelle());
    }

    private List<StoreInventoryGroupExport> buildStoreInventoryGroupExportsFromTuple(
        List<Tuple> tuples,
        StoreInventoryExportRecord storeInventoryExportRecord
    ) {
        LinkedHashMap<Long, StoreInventoryGroupExport> map = new LinkedHashMap<>();
        StoreInventoryGroupExport storeInventoryGroupExport;
        switch (storeInventoryExportRecord.exportGroupBy()) {
            case FAMILLY -> {
                for (Tuple t : tuples) {
                    Long id = t.get("famillyId", Long.class);
                    StoreInventoryLineExport storeInventoryLineExport = StoreInventoryLineFilterBuilder.buildStoreInventoryLineExportRecord(
                        t
                    );
                    storeInventoryGroupExport = map.get(id);
                    storeInventoryGroupExport = buildFromTuple(
                        storeInventoryGroupExport,
                        id,
                        storeInventoryLineExport.getFamillyCode(),
                        storeInventoryLineExport.getFamillyLibelle(),
                        storeInventoryLineExport
                    );
                    map.put(id, storeInventoryGroupExport);
                }
            }
            case RAYON, NONE -> {
                for (Tuple t : tuples) {
                    Long idRayon = t.get("rayon_id", Long.class);

                    StoreInventoryLineExport storeInventoryLineExport = StoreInventoryLineFilterBuilder.buildStoreInventoryLineExportRecord(
                        t
                    );
                    Triple<Long, String, String> integerStringStringTriple = getRayon(idRayon, storeInventoryLineExport);
                    long id = integerStringStringTriple.getLeft();
                    storeInventoryGroupExport = map.get(id);
                    storeInventoryGroupExport = buildFromTuple(
                        storeInventoryGroupExport,
                        id,
                        integerStringStringTriple.getMiddle(),
                        integerStringStringTriple.getRight(),
                        storeInventoryLineExport
                    );
                    map.put(id, storeInventoryGroupExport);
                }
            }
            case STORAGE -> {
                for (Tuple t : tuples) {
                    long id = t.get("storage_id", Long.class);
                    StoreInventoryLineExport storeInventoryLineExport = StoreInventoryLineFilterBuilder.buildStoreInventoryLineExportRecord(
                        t
                    );
                    storeInventoryGroupExport = map.get(id);
                    storeInventoryGroupExport = buildFromTuple(
                        storeInventoryGroupExport,
                        id,
                        storeInventoryLineExport.getStorageLibelle(),
                        storeInventoryLineExport.getStorageLibelle(),
                        storeInventoryLineExport
                    );
                    map.put(id, storeInventoryGroupExport);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public void remove(Long id) {
        storeInventoryRepository.deleteById(id);
    }

    @Override
    public StoreInventoryLineRecord updateQuantityOnHand(StoreInventoryLineDTO storeInventoryLineDTO) {
        StoreInventoryLine storeInventoryLine = storeInventoryLineRepository.getReferenceById(storeInventoryLineDTO.getId());
        updateStoreInventoryLine(storeInventoryLineDTO, storeInventoryLine);

        storeInventoryLine = storeInventoryLineRepository.saveAndFlush(storeInventoryLine);
        return new StoreInventoryLineRecord(
            storeInventoryLineDTO.getProduitId().intValue(),
            storeInventoryLineDTO.getProduitCip(),
            storeInventoryLineDTO.getProduitEan(),
            storeInventoryLineDTO.getProduitLibelle(),
            storeInventoryLine.getId(),
            storeInventoryLine.getGap(),
            storeInventoryLine.getQuantityOnHand(),
            storeInventoryLine.getQuantityInit(),
            storeInventoryLine.getUpdated(),
            storeInventoryLine.getInventoryValueCost(),
            storeInventoryLine.getLastUnitPrice()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoreInventoryDTO> getStoreInventory(Long id) {
        return storeInventoryRepository.findById(id).map(StoreInventoryDTO::new);
    }

    @Override
    public StoreInventoryDTO create(StoreInventoryRecord storeInventoryRecord) {
        StoreInventory storeInventory = new StoreInventory();
        storeInventory.setCalendar(this.warehouseCalendarService.initCalendar());
        storeInventory.createdAt(LocalDateTime.now());
        storeInventory.updatedAt(storeInventory.getCreatedAt());
        storeInventory.setUser(userService.getUser());
        storeInventory.setInventoryCategory(InventoryCategory.valueOf(storeInventoryRecord.inventoryCategory()));
        storeInventory.setInventoryAmountAfter(0L);
        storeInventory.setInventoryAmountBegin(0L);
        storeInventory.setInventoryValueCostBegin(0L);
        storeInventory.setInventoryValueCostAfter(0L);
        storeInventory.setGapAmount(0);
        storeInventory.setGapCost(0);
        if (Objects.isNull(storeInventoryRecord.storage())) {
            storeInventory.setStorage(this.storageService.getDefaultConnectedUserMainStorage());
        }

        if (storeInventory.getInventoryCategory() == InventoryCategory.RAYON) {
            Rayon rayon = this.rayonRepository.getReferenceById(storeInventoryRecord.rayon());
            storeInventory.setRayon(rayon);
            storeInventory.setStorage(rayon.getStorage());
        }
        if (Objects.isNull(storeInventory.getStorage())) {
            storeInventory.setStorage(this.storageService.getOne(storeInventoryRecord.storage()));
        }
        storeInventory = this.storeInventoryRepository.saveAndFlush(storeInventory);
        insertItems(storeInventory, storeInventoryRecord);
        return new StoreInventoryDTO(storeInventory);
    }

    private String buildInsertQuery(StoreInventory storeInventory, StoreInventoryRecord storeInventoryRecord) {
        return switch (storeInventory.getInventoryCategory()) {
            case MAGASIN -> String.format(StoreInventoryLineFilterBuilder.SQL_ALL_INSERT_ALL, storeInventory.getId()).replace(
                "{famille_close}",
                ""
            );
            case RAYON -> String.format(StoreInventoryLineFilterBuilder.SQL_ALL_INSERT, storeInventory.getId()) +
            String.format(" AND r.id=%d ", storeInventory.getRayon().getId());
            case STORAGE -> String.format(StoreInventoryLineFilterBuilder.SQL_ALL_INSERT, storeInventory.getId()) +
            String.format(" AND s.id=%d ", storeInventory.getStorage().getId());
            case FAMILLY -> String.format(StoreInventoryLineFilterBuilder.SQL_ALL_INSERT_ALL, storeInventory.getId()).replace(
                "{famille_close}",
                String.format(" AND p.famille_id=%d", storeInventoryRecord.famillyId())
            );
        };
    }

    private void insertItems(StoreInventory storeInventory, StoreInventoryRecord storeInventoryRecord) {
        this.em.createNativeQuery(buildInsertQuery(storeInventory, storeInventoryRecord)).executeUpdate();
    }

    private List<Predicate> predicates(
        StoreInventoryFilterRecord storeInventoryFilterRecord,
        CriteriaBuilder cb,
        Root<StoreInventory> root
    ) {
        List<Predicate> predicates = new ArrayList<>();
        In<InventoryCategory> inventoryCategoryIn = cb.in(root.get(StoreInventory_.inventoryCategory));
        storeInventoryFilterRecord.inventoryCategories().forEach(inventoryCategoryIn::value);
        predicates.add(inventoryCategoryIn);
        In<InventoryStatut> inventoryStatutIn = cb.in(root.get(StoreInventory_.statut));
        storeInventoryFilterRecord.statuts().forEach(inventoryStatutIn::value);
        predicates.add(inventoryStatutIn);
        if (Objects.nonNull(storeInventoryFilterRecord.rayonId())) {
            predicates.add(cb.equal(root.get(StoreInventory_.rayon).get(Rayon_.id), storeInventoryFilterRecord.rayonId()));
        }
        if (Objects.nonNull(storeInventoryFilterRecord.storageId())) {
            predicates.add(cb.equal(root.get(StoreInventory_.storage).get(Storage_.id), storeInventoryFilterRecord.storageId()));
        }
        if (Objects.nonNull(storeInventoryFilterRecord.userId())) {
            predicates.add(cb.equal(root.get(StoreInventory_.user).get(User_.id), storeInventoryFilterRecord.userId()));
        }

        return predicates;
    }

    private List<StoreInventory> fetchInventories(StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<StoreInventory> cq = cb.createQuery(StoreInventory.class);
        Root<StoreInventory> root = cq.from(StoreInventory.class);
        cq.select(root).orderBy(cb.desc(root.get(StoreInventory_.updatedAt)));
        List<Predicate> predicates = predicates(storeInventoryFilterRecord, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<StoreInventory> q = em.createQuery(cq);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return q.getResultList();
    }

    public long fetchInventoriesCount(StoreInventoryFilterRecord storeInventoryFilterRecord) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<StoreInventory> root = cq.from(StoreInventory.class);
        cq.select(cb.count(root));
        List<Predicate> predicates = predicates(storeInventoryFilterRecord, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;
    }

    private long getTotalCount(StoreInventory storeInventory, StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
        try {
            return (Long) this.em.createNativeQuery(this.buildFetchDetailQueryCount(storeInventoryLineFilterRecord))
                .setParameter(1, storeInventory.getId())
                .getSingleResult();
        } catch (Exception e) {
            log.error(null, e);
            return 0;
        }
    }

    private List<Tuple> getAllByInventories(StoreInventoryLineFilterRecord storeInventoryLineFilterRecord, Pageable pageable) {
        try {
            return this.em.createNativeQuery(this.buildFetchDetailQuery(storeInventoryLineFilterRecord), Tuple.class)
                .setParameter(1, storeInventoryLineFilterRecord.storeInventoryId())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        } catch (Exception e) {
            log.error(null, e);
        }
        return Collections.emptyList();
    }

    private List<StoreInventoryLineRecord> buildItems(
        StoreInventory storeInventory,
        StoreInventoryLineFilterRecord storeInventoryLineFilterRecord,
        Pageable pageable
    ) {
        return getAllByInventories(storeInventoryLineFilterRecord, pageable)
            .stream()
            .map(tuple -> buildStoreInventoryLineRecordRecord(tuple, storeInventory))
            .toList();
    }

    StoreInventoryLineRecord buildStoreInventoryLineRecordRecord(Tuple tuple, StoreInventory storeInventory) {
        if (Objects.isNull(tuple.get("produitId", Long.class))) {
            return null;
        }
        boolean updated = tuple.get("updated", Boolean.class);
        int currentStock = getStock(storeInventory, tuple.get("produitId", Long.class));
        return new StoreInventoryLineRecord(
            tuple.get("produitId", Long.class).intValue(),
            tuple.get("code_cip", String.class),
            tuple.get("code_ean", String.class),
            tuple.get("libelle", String.class),
            tuple.get("id", Long.class),
            tuple.get("gap", Integer.class),
            tuple.get("quantity_on_hand", Integer.class),
            currentStock,
            updated,
            tuple.get("prix_achat", Integer.class),
            tuple.get("prix_uni", Integer.class)
        );
    }

    private int getStock(StoreInventory storeInventory, long produitId) {
        StockProduit stockProduit =
            this.stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storeInventory.getStorage().getId());
        return Objects.nonNull(stockProduit.getQtyUG()) ? stockProduit.getQtyUG() + stockProduit.getQtyStock() : stockProduit.getQtyStock();
    }

    private List<Tuple> getAllByInventories(StoreInventoryExportRecord inventoryExportRecord) {
        try {
            return this.em.createNativeQuery(this.buildExportQuery(inventoryExportRecord), Tuple.class)
                .setParameter(1, inventoryExportRecord.filterRecord().storeInventoryId())
                .getResultList();
        } catch (Exception e) {
            log.error(null, e);
        }
        return Collections.emptyList();
    }

    // @EventListener(ApplicationReadyEvent.class)
    private void updateAll() {
        AtomicInteger atomicInteger = new AtomicInteger(5);
        this.storeInventoryLineRepository.findAllByStoreInventoryId(-1L).forEach(storeInventory -> {
                StoreInventoryLineDTO dto = new StoreInventoryLineDTO();
                dto.setId(storeInventory.getId());
                dto.setQuantitySold(0);
                dto.setQuantityInit(0);
                dto.setProduitId(0L);
                dto.setQuantityOnHand(atomicInteger.getAndIncrement());
                updateQuantityOnHand(dto);
            });
    }
}
