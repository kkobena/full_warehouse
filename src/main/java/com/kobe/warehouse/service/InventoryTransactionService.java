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
import com.kobe.warehouse.service.criteria.InventoryTransactionSpec;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import com.kobe.warehouse.service.dto.filter.InventoryTransactionFilterDTO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  private final InventoryTransactionSpec inventoryTransactionSpec;

  public InventoryTransactionService(
      InventoryTransactionRepository inventoryTransactionRepository,
      ProduitRepository produitRepository,
      InventoryTransactionSpec inventoryTransactionSpec) {
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.produitRepository = produitRepository;
    this.inventoryTransactionSpec = inventoryTransactionSpec;
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

  
  public void buildInventoryTransaction(
      StoreInventoryLine storeInventoryLine, LocalDateTime now, User user) {
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
    inventoryTransaction.setCreatedAt(LocalDateTime.now());
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

  @Transactional(readOnly = true)
  public Page<InventoryTransactionDTO> getAllInventoryTransactions(
      Pageable pageable, Long produitId, String startDate, String endDate, Integer type) {
    this.inventoryTransactionSpec.setInventoryTransactionFilter(
        InventoryTransactionFilterDTO.builder()
            .endDate(endDate)
            .produitId(produitId)
            .startDate(startDate)
            .type(type)
            .build());

    return inventoryTransactionRepository
        .findAll(this.inventoryTransactionSpec, pageable)
        .map(InventoryTransactionDTO::new);
  }
}
