package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class InventoryTransactionService {
    private final Logger LOG = LoggerFactory.getLogger(InventoryTransactionService.class);
    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired
    private ProduitRepository produitRepository;

    @Transactional(readOnly = true)
    public long quantitySold(Long produitId) {
        Long aLong=inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
        return (aLong!=null?aLong:0);
    }

    @Transactional(readOnly = true)
    public long quantitySoldIncludeChildQuantity(Long produitId) {
        Produit produit = produitRepository.getOne(produitId);
        long parentQty = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
        if (!produit.getProduits().isEmpty()) {
            long childQty = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produit.getProduits().get(0).getId());
            parentQty += ((long) Math.ceil(Double.valueOf(childQty) / produit.getItemQty()));
        }
        return parentQty;
    }
    public void buildInventoryTransaction(StoreInventoryLine storeInventoryLine, DateDimension dateDimension, Instant now, User user) {
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setCreatedAt(now);
        inventoryTransaction.setUpdatedAt(inventoryTransaction.getCreatedAt());
        inventoryTransaction.setProduit(storeInventoryLine.getProduit());
        inventoryTransaction.setUser(user);
        inventoryTransaction.setAmount(storeInventoryLine.getInventoryValueCost());
        inventoryTransaction.setQuantity(storeInventoryLine.getUpdated()?storeInventoryLine.getQuantityOnHand():storeInventoryLine.getQuantityInit());
        inventoryTransaction.setTransactionType(TransactionType.INVENTAIRE);
        inventoryTransaction.setQuantityAfter(inventoryTransaction.getQuantity());
        inventoryTransaction.setQuantityBefor(storeInventoryLine.getQuantityInit());
        inventoryTransaction.setDateDimension(dateDimension);
        inventoryTransaction.setCostAmount(storeInventoryLine.getProduit().getCostAmount());
        inventoryTransaction.setRegularUnitPrice(storeInventoryLine.getInventoryValueLatestSellingPrice());
        inventoryTransactionRepository.save(inventoryTransaction);

    }
}
