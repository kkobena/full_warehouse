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
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.filter.StoreInventoryFilterRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import com.kobe.warehouse.web.rest.errors.InventoryException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventaireServiceImpl implements InventaireService {

  private static final Comparator<StoreInventoryLineDTO> COMPARATOR_LINE =
      Comparator.comparing(StoreInventoryLineDTO::getProduitLibelle);
  private final Logger LOG = LoggerFactory.getLogger(InventaireServiceImpl.class);

  private final UserService userService;

  private final StoreInventoryRepository storeInventoryRepository;

  private final StoreInventoryLineRepository storeInventoryLineRepository;
  private final StorageService storageService;
  private final StockProduitRepository stockProduitRepository;
  private final RayonRepository rayonRepository;
  @PersistenceContext private EntityManager em;

  public InventaireServiceImpl(
      UserService userService,
      StoreInventoryRepository storeInventoryRepository,
      StoreInventoryLineRepository storeInventoryLineRepository,
      StorageService storageService,
      StockProduitRepository stockProduitRepository,
      RayonRepository rayonRepository) {

    this.userService = userService;
    this.storeInventoryRepository = storeInventoryRepository;
    this.storeInventoryLineRepository = storeInventoryLineRepository;
    this.storageService = storageService;
    this.stockProduitRepository = stockProduitRepository;
    this.rayonRepository = rayonRepository;
  }

  @Override
  public ItemsCountRecord close(Long id) throws InventoryException {

    long count =
        storeInventoryLineRepository.countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(id);
    if (count > 0) throw new InventoryException();
    StoreInventory storeInventory = storeInventoryRepository.getReferenceById(id);
    storeInventory.setStatut(InventoryStatut.CLOSED);
    storeInventory.setUpdatedAt(LocalDateTime.now());
    StoreInventorySummaryRecord storeInventorySummaryRecord = fetchSummary(id);
    storeInventory.setInventoryAmountBegin(
        storeInventorySummaryRecord.amountValueBegin().longValue());
    storeInventory.setInventoryValueCostAfter(
        storeInventorySummaryRecord.costValueAfter().longValue());
    storeInventory.setInventoryAmountAfter(
        storeInventorySummaryRecord.amountValueAfter().longValue());
    storeInventory.setInventoryValueCostBegin(
        storeInventorySummaryRecord.costValueBegin().longValue());
    int itemCount = closeItems(storeInventory.getId());
    storeInventoryRepository.save(storeInventory);
    return new ItemsCountRecord(itemCount);
  }

  private int closeItems(Long id) {
    return this.storeInventoryLineRepository.procCloseInventory(id.intValue());
  }

  private StoreInventorySummaryRecord fetchSummary(Long id) {
    return StoreInventoryLineFilterBuilder.buildSammary(
        (Tuple)
            this.em
                .createNativeQuery(StoreInventoryLineFilterBuilder.SUMMARY_SQL, Tuple.class)
                .setParameter(1, id)
                .getSingleResult());
  }

  private void updateStoreInventoryLine(
      StoreInventoryLineDTO storeInventoryLineDTO, StoreInventoryLine storeInventoryLine) {
    Produit produit = storeInventoryLine.getProduit();
    FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
    storeInventoryLine.setQuantitySold(0);
    storeInventoryLine.setUpdated(true);
    storeInventoryLine.setUpdatedAt(LocalDateTime.now());
    storeInventoryLine.setInventoryValueCost(
        Objects.nonNull(fournisseurProduit)
            ? fournisseurProduit.getPrixAchat()
            : produit.getCostAmount());
    storeInventoryLine.setLastUnitPrice(
        Objects.nonNull(fournisseurProduit)
            ? fournisseurProduit.getPrixUni()
            : produit.getRegularUnitPrice());
    storeInventoryLine.setQuantityOnHand(storeInventoryLineDTO.getQuantityOnHand());
    storeInventoryLine.setQuantityInit(storeInventoryLineDTO.getQuantityInit());
    storeInventoryLine.setGap(
        storeInventoryLine.getQuantityOnHand() - storeInventoryLine.getQuantityInit());
  }

  @Transactional(readOnly = true)
  @Override
  public Page<StoreInventoryLineRecord> getAllByInventory(
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord, Pageable pageable) {
    StoreInventory storeInventory =
        this.storeInventoryRepository.getReferenceById(
            storeInventoryLineFilterRecord.storeInventoryId());
    if (storeInventory.getStatut() == InventoryStatut.CLOSED) return Page.empty(pageable);

    long count = getTotalCount(storeInventory, storeInventoryLineFilterRecord);
    if (count == 0) return Page.empty(pageable);
    return new PageImpl<>(
        buildItems(storeInventory, storeInventoryLineFilterRecord, pageable), pageable, count);
  }

  @Transactional(readOnly = true)
  @Override
  public Page<StoreInventoryDTO> storeInventoryList(
      StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable) {
    return new PageImpl<>(
        this.fetchInventories(storeInventoryFilterRecord, pageable).stream()
            .map(StoreInventoryDTO::new)
            .toList(),
        pageable,
        this.fetchInventoriesCount(storeInventoryFilterRecord));
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
  public void remove(Long id) {
    storeInventoryRepository.deleteById(id);
  }

  @Override
  public StoreInventoryLineRecord updateQuantityOnHand(
      StoreInventoryLineDTO storeInventoryLineDTO) {

    StoreInventoryLine storeInventoryLine =
        storeInventoryLineRepository.getReferenceById(storeInventoryLineDTO.getId());
    updateStoreInventoryLine(storeInventoryLineDTO, storeInventoryLine);

    storeInventoryLine = storeInventoryLineRepository.saveAndFlush(storeInventoryLine);
    return new StoreInventoryLineRecord(
        storeInventoryLineDTO.getProduitId().intValue(),
        storeInventoryLineDTO.getProduitCip(),
        storeInventoryLineDTO.getProduitEan(),
        storeInventoryLineDTO.getProduitLibelle(),
        BigInteger.valueOf(storeInventoryLine.getId()),
        storeInventoryLine.getGap(),
        storeInventoryLine.getQuantityOnHand(),
        storeInventoryLine.getQuantityInit(),
        storeInventoryLine.getUpdated(),
        storeInventoryLine.getInventoryValueCost(),
        storeInventoryLine.getLastUnitPrice());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StoreInventoryDTO> getStoreInventory(Long id) {
    return storeInventoryRepository.findById(id).map(StoreInventoryDTO::new);
  }

  @Override
  public StoreInventoryDTO create(StoreInventoryRecord storeInventoryRecord) {
    StoreInventory storeInventory = new StoreInventory();
    storeInventory.createdAt(LocalDateTime.now());
    storeInventory.updatedAt(storeInventory.getCreatedAt());
    storeInventory.setUser(userService.getUser());
    storeInventory.setInventoryCategory(
        InventoryCategory.valueOf(storeInventoryRecord.inventoryCategory()));
    storeInventory.setInventoryAmountAfter(0L);
    storeInventory.setInventoryAmountBegin(0L);
    storeInventory.setInventoryValueCostBegin(0L);
    storeInventory.setInventoryValueCostAfter(0L);
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
    insertItems(storeInventory);
    return new StoreInventoryDTO(storeInventory);
  }

  private String buildInsertQuery(StoreInventory storeInventory) {

    return switch (storeInventory.getInventoryCategory()) {
      case MAGASIN -> String.format(
          StoreInventoryLineFilterBuilder.SQL_ALL_INSERT_ALL, storeInventory.getId());
      case RAYON -> String.format(
              StoreInventoryLineFilterBuilder.SQL_ALL_INSERT, storeInventory.getId())
          + String.format(" AND r.id=%d ", storeInventory.getRayon().getId());
      case STORAGE -> String.format(
              StoreInventoryLineFilterBuilder.SQL_ALL_INSERT, storeInventory.getId())
          + String.format(" AND s.id=%d ", storeInventory.getStorage().getId());
    };
  }

  private void insertItems(StoreInventory storeInventory) {
    this.em.createNativeQuery(buildInsertQuery(storeInventory)).executeUpdate();
  }

  private List<Predicate> predicates(
      StoreInventoryFilterRecord storeInventoryFilterRecord,
      CriteriaBuilder cb,
      Root<StoreInventory> root) {
    List<Predicate> predicates = new ArrayList<>();
    In<InventoryCategory> inventoryCategoryIn = cb.in(root.get(StoreInventory_.inventoryCategory));
    storeInventoryFilterRecord.inventoryCategories().forEach(s -> inventoryCategoryIn.value(s));
    predicates.add(inventoryCategoryIn);
    In<InventoryStatut> inventoryStatutIn = cb.in(root.get(StoreInventory_.statut));
    storeInventoryFilterRecord.statuts().forEach(s -> inventoryStatutIn.value(s));
    predicates.add(inventoryStatutIn);
    if (Objects.nonNull(storeInventoryFilterRecord.rayonId())) {
      predicates.add(
          cb.equal(
              root.get(StoreInventory_.rayon).get(Rayon_.id),
              storeInventoryFilterRecord.rayonId()));
    }
    if (Objects.nonNull(storeInventoryFilterRecord.storageId())) {
      predicates.add(
          cb.equal(
              root.get(StoreInventory_.storage).get(Storage_.id),
              storeInventoryFilterRecord.storageId()));
    }
    if (Objects.nonNull(storeInventoryFilterRecord.userId())) {
      predicates.add(
          cb.equal(
              root.get(StoreInventory_.user).get(User_.id), storeInventoryFilterRecord.userId()));
    }

    return predicates;
  }

  private List<StoreInventory> fetchInventories(
      StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable) {
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

  private long getTotalCount(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {

    try {
      return ((BigInteger)
              this.em
                  .createNativeQuery(
                      this.buildFetchDetailQueryCount(storeInventoryLineFilterRecord))
                  .setParameter(1, storeInventory.getId())
                  .getSingleResult())
          .intValue();

    } catch (Exception e) {
      LOG.error(null, e);
      return 0;
    }
  }

  private List<Tuple> getAllByInventory(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord,
      Pageable pageable) {

    try {
      return this.em
          .createNativeQuery(
              this.buildFetchDetailQuery(storeInventoryLineFilterRecord), Tuple.class)
          .setParameter(1, storeInventoryLineFilterRecord.storeInventoryId())
          .setFirstResult((int) pageable.getOffset())
          .setMaxResults(pageable.getPageSize())
          .getResultList();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return Collections.emptyList();
  }

  private List<StoreInventoryLineRecord> buildItems(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord,
      Pageable pageable) {
    return getAllByInventory(storeInventory, storeInventoryLineFilterRecord, pageable).stream()
        .map(
            tuple ->
                StoreInventoryLineFilterBuilder.buildStoreInventoryLineRecordRecord(
                    tuple,
                    getStock(storeInventory, tuple.get("produitId", BigInteger.class).longValue())))
        .toList();
  }

  private int getStock(StoreInventory storeInventory, long produitId) {
    StockProduit stockProduit =
        this.stockProduitRepository.findOneByProduitIdAndStockageId(
            produitId, storeInventory.getStorage().getId());
    return Objects.nonNull(stockProduit.getQtyUG())
        ? stockProduit.getQtyUG() + stockProduit.getQtyStock()
        : stockProduit.getQtyStock();
  }
}
