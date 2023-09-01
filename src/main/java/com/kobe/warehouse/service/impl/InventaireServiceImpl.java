package com.kobe.warehouse.service.impl;

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
import com.kobe.warehouse.repository.ProduitRepository;
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
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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

  private final ProduitRepository produitRepository;

  private final UserService userService;

  private final StoreInventoryRepository storeInventoryRepository;

  private final StoreInventoryLineRepository storeInventoryLineRepository;
  private final StorageService storageService;
  private final StockProduitRepository stockProduitRepository;
  @PersistenceContext private EntityManager em;

  public InventaireServiceImpl(
      ProduitRepository produitRepository,
      UserService userService,
      StoreInventoryRepository storeInventoryRepository,
      StoreInventoryLineRepository storeInventoryLineRepository,
      StorageService storageService,
      StockProduitRepository stockProduitRepository) {
    this.produitRepository = produitRepository;
    this.userService = userService;
    this.storeInventoryRepository = storeInventoryRepository;
    this.storeInventoryLineRepository = storeInventoryLineRepository;
    this.storageService = storageService;
    this.stockProduitRepository = stockProduitRepository;
  }

  @Override
  public void close(Long id) {
    long inventoryValueCostAfter = 0, inventoryAmountAfter = 0;
    StoreInventory storeInventory = storeInventoryRepository.getReferenceById(id);

    storeInventory.setStatut(InventoryStatut.CLOSED);
    storeInventory.setUpdatedAt(LocalDateTime.now());
    List<StoreInventoryLine> storeInventoryLines =
        storeInventoryLineRepository.findAllByStoreInventoryId(id);
    for (StoreInventoryLine line : storeInventoryLines) {
      inventoryValueCostAfter += ((long) line.getInventoryValueCost() * line.getQuantityOnHand());
      inventoryAmountAfter +=
          ((long) line.getInventoryValueLatestSellingPrice() * line.getQuantityOnHand());
      Produit produit = line.getProduit();
      // produit.setQuantity(line.getUpdated() ? line.getQuantityOnHand() : line.getQuantityInit());
      produitRepository.save(produit);
    }
    storeInventory.setInventoryValueCostAfter(inventoryValueCostAfter);
    storeInventory.setInventoryAmountAfter(inventoryAmountAfter);
    storeInventoryRepository.save(storeInventory);
    storeInventoryLineRepository.saveAll(storeInventoryLines);
  }

  private StoreInventoryLine createStoreInventoryLine(StoreInventoryLineDTO storeInventoryLineDTO) {
    Produit produit = this.produitRepository.getReferenceById(storeInventoryLineDTO.getProduitId());
    StoreInventory storeInventory =
        this.storeInventoryRepository.getReferenceById(storeInventoryLineDTO.getStoreInventoryId());
    StoreInventoryLine storeInventoryLine = new StoreInventoryLine();
    storeInventoryLine.setProduit(produit);
    storeInventoryLine.setQuantitySold(0);
    storeInventoryLine.setStoreInventory(storeInventory);
    storeInventoryLine.setQuantityInit(storeInventoryLineDTO.getQuantityInit());
    storeInventoryLine.setQuantityOnHand(storeInventoryLineDTO.getQuantityOnHand());
    storeInventoryLine.setUpdated(true);
    storeInventoryLine.setInventoryValueCost(storeInventoryLineDTO.getPrixAchat());
    storeInventoryLine.setInventoryValueLatestSellingPrice(storeInventoryLineDTO.getPrixUni());
    storeInventoryLine.setGap(
        storeInventoryLine.getQuantityOnHand() - storeInventoryLine.getQuantityInit());
    return storeInventoryLine;
  }

  @Transactional(readOnly = true)
  @Override
  public List<StoreInventoryLineDTO> storeInventoryList(Long storeInventoryId) {
    return storeInventoryLineRepository.findAllByStoreInventoryId(storeInventoryId).stream()
        .map(StoreInventoryLineDTO::new)
        .sorted(COMPARATOR_LINE)
        .collect(Collectors.toList());
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
    StoreInventoryLine storeInventoryLine;
    if (Objects.isNull(storeInventoryLineDTO.getId())) {
      storeInventoryLine = createStoreInventoryLine(storeInventoryLineDTO);
    } else {
      storeInventoryLine =
          storeInventoryLineRepository.getReferenceById(storeInventoryLineDTO.getId());
      storeInventoryLine.setQuantityOnHand(storeInventoryLineDTO.getQuantityOnHand());
      storeInventoryLine.setGap(
          storeInventoryLine.getQuantityOnHand() - storeInventoryLine.getQuantityInit());
    }
    storeInventoryLineRepository.saveAndFlush(storeInventoryLine);
    return new StoreInventoryLineRecord(
        storeInventoryLineDTO.getProduitId().intValue(),
        storeInventoryLineDTO.getProduitCip(),
        storeInventoryLineDTO.getProduitEan(),
        storeInventoryLineDTO.getProduitLibelle(),
        BigInteger.valueOf(storeInventoryLine.getId()),
        storeInventoryLine.getGap(),
        storeInventoryLine.getQuantityOnHand(),
        storeInventoryLine.getQuantityInit(),
        true,
        storeInventoryLineDTO.getPrixAchat(),
        storeInventoryLineDTO.getPrixUni());
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
    } else {
      storeInventory.setStorage(this.storageService.getOne(storeInventoryRecord.storage()));
    }
    if (storeInventory.getInventoryCategory() == InventoryCategory.RAYON) {
      storeInventory.setRayon(rayonFromId(storeInventoryRecord.rayon()));
    }
    return new StoreInventoryDTO(this.storeInventoryRepository.saveAndFlush(storeInventory));
  }

  private Rayon rayonFromId(Long id) {
    if (Objects.nonNull(id)) {
      return new Rayon().id(id);
    }
    return null;
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
                      this.buildFetchDetailQueryCount(
                          storeInventory, storeInventoryLineFilterRecord))
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
              this.buildFetchDetailQuery(storeInventory, storeInventoryLineFilterRecord),
              Tuple.class)
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
