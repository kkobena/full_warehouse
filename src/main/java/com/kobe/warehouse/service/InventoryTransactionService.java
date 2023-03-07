package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryTransactionService {
  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private final Logger LOG = LoggerFactory.getLogger(InventoryTransactionService.class);

  private final InventoryTransactionRepository inventoryTransactionRepository;

  private final ProduitRepository produitRepository;

  public InventoryTransactionService(
      InventoryTransactionRepository inventoryTransactionRepository,
      ProduitRepository produitRepository) {
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.produitRepository = produitRepository;
  }

  @Transactional(readOnly = true)
  public long quantitySold(Long produitId) {
    Long aLong = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
    return (aLong != null ? aLong : 0);
  }

  @Transactional(readOnly = true)
  public long quantitySoldIncludeChildQuantity(Long produitId) {
    Produit produit = produitRepository.getReferenceById(produitId);
    long parentQty = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
    if (!produit.getProduits().isEmpty()) {
      long childQty =
          inventoryTransactionRepository.quantitySold(
              TransactionType.SALE, produit.getProduits().get(0).getId());
      parentQty += ((long) Math.ceil(Double.valueOf(childQty) / produit.getItemQty()));
    }
    return parentQty;
  }

  @Transactional(readOnly = true)
  public Optional<InventoryTransaction> findById(Long id) {
    return inventoryTransactionRepository.findById(id);
  }

  private Specification<InventoryTransaction> add(
      Specification<InventoryTransaction> specification,
      Specification<InventoryTransaction> current) {
    if (current == null) {
      current = Specification.where(specification);
    } else {
      current = specification.and(specification);
    }
    return current;
  }

  @Transactional(readOnly = true)
  public List<InventoryTransactionDTO> getAllInventoryTransactions(
      Long produitId, String startDate, String endDate, Integer type) {
    Instant dtStart = null;
    Instant dtEnd = null;
    Specification<InventoryTransaction> specification = null;
    if (type != null && type != -1) {
      TransactionType transactionType = TransactionType.values()[type];
      specification =
          Specification.where(
              inventoryTransactionRepository.specialisationTypeTransaction(transactionType));
    }
    if (produitId != null) {
      add(inventoryTransactionRepository.specialisationProduitId(produitId), specification);
    }
    if (org.apache.commons.lang3.StringUtils.isEmpty(startDate)
        || org.apache.commons.lang3.StringUtils.isEmpty(endDate)) {
      if (org.apache.commons.lang3.StringUtils.isNotEmpty(startDate)) {
        LocalDateTime dateStart =
            LocalDateTime.of(LocalDate.parse(startDate, dateTimeFormatter), LocalTime.of(0, 0, 0));
        dtStart = dateStart.toInstant(ZoneOffset.UTC);
        add(
            inventoryTransactionRepository.specialisationDateGreaterThanOrEqualTo(dtStart),
            specification);
      }
      if (org.apache.commons.lang3.StringUtils.isNotEmpty(endDate)) {
        LocalDateTime dateEnd =
            LocalDateTime.of(LocalDate.parse(endDate, dateTimeFormatter), LocalTime.of(23, 59, 59));
        dtEnd = dateEnd.toInstant(ZoneOffset.UTC);
        add(
            inventoryTransactionRepository.specialisationDateLessThanOrEqualTo(dtEnd),
            specification);
      }
    } else {
      add(inventoryTransactionRepository.specialisationDateMvt(dtStart, dtEnd), specification);
    }
    return inventoryTransactionRepository
        .findAll(specification, Sort.by(Sort.Direction.ASC, "createdAt"))
        .stream()
        .map(InventoryTransactionDTO::new)
        .collect(Collectors.toList());
  }

  public void buildInventoryTransaction(
      StoreInventoryLine storeInventoryLine, Instant now, User user) {
    InventoryTransaction inventoryTransaction = new InventoryTransaction();
    inventoryTransaction.setCreatedAt(now);
    inventoryTransaction.setProduit(storeInventoryLine.getProduit());
    inventoryTransaction.setUser(user);
    inventoryTransaction.setQuantity(
        storeInventoryLine.getUpdated()
            ? storeInventoryLine.getQuantityOnHand()
            : storeInventoryLine.getQuantityInit());
    inventoryTransaction.setTransactionType(TransactionType.INVENTAIRE);
    inventoryTransaction.setQuantityAfter(inventoryTransaction.getQuantity());
    inventoryTransaction.setQuantityBefor(storeInventoryLine.getQuantityInit());
    inventoryTransaction.setCostAmount(storeInventoryLine.getProduit().getCostAmount());
    inventoryTransaction.setRegularUnitPrice(
        storeInventoryLine.getInventoryValueLatestSellingPrice());
    inventoryTransactionRepository.save(inventoryTransaction);
  }

  public void saveInventoryTransaction(DeliveryReceiptItem deliveryReceiptItem, User user) {
    FournisseurProduit fournisseurProduit = deliveryReceiptItem.getFournisseurProduit();
    InventoryTransaction inventoryTransaction = new InventoryTransaction();
    inventoryTransaction.setCreatedAt(Instant.now());
    inventoryTransaction.setDeliveryReceiptItem(deliveryReceiptItem);
    inventoryTransaction.setFournisseurProduit(fournisseurProduit);
    inventoryTransaction.setProduit(fournisseurProduit.getProduit());
    inventoryTransaction.setUser(user);
    inventoryTransaction.setMagasin(user.getMagasin());
    inventoryTransaction.setQuantity(
        deliveryReceiptItem.getQuantityReceived() + deliveryReceiptItem.getUgQuantity());
    inventoryTransaction.setTransactionType(TransactionType.ENTREE_STOCK);
    inventoryTransaction.setQuantityAfter(
        inventoryTransaction.getQuantity() + deliveryReceiptItem.getInitStock());
    inventoryTransaction.setQuantityBefor(deliveryReceiptItem.getInitStock());
    inventoryTransaction.setCostAmount(deliveryReceiptItem.getOrderCostAmount());
    inventoryTransaction.setRegularUnitPrice(deliveryReceiptItem.getOrderUnitPrice());
    inventoryTransactionRepository.save(inventoryTransaction);
  }
}
