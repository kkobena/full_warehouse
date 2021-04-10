package com.kobe.warehouse.repository;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.TransactionType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the InventoryTransaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

	List<InventoryTransaction> findByProduitId(Long produitId, Sort sort);

	default InventoryTransaction buildInventoryTransaction(OrderLine orderLine, User user) {
		InventoryTransaction inventoryTransaction = new InventoryTransaction();
		inventoryTransaction.setCreatedAt(orderLine.getCreatedAt());
		inventoryTransaction.setUpdatedAt(orderLine.getCreatedAt());
		inventoryTransaction.setProduit(orderLine.getProduit());
		inventoryTransaction.setUser(user);
		inventoryTransaction.dateDimension(Constants.DateDimension(LocalDate.now()));
		inventoryTransaction.setAmount(orderLine.getOrderAmount());
		inventoryTransaction.setQuantity(orderLine.getQuantityReceived());
		inventoryTransaction.setTransactionType(TransactionType.COMMANDE);
		return inventoryTransaction;
	}

	default InventoryTransaction buildInventoryTransaction(SalesLine salesLine, User user) {
		InventoryTransaction inventoryTransaction = new InventoryTransaction();
		inventoryTransaction.setCreatedAt(Instant.now());
		inventoryTransaction.setUpdatedAt(inventoryTransaction.getCreatedAt());
		inventoryTransaction.setProduit(salesLine.getProduit());
		inventoryTransaction.setUser(user);
		inventoryTransaction.setAmount(salesLine.getSalesAmount());
		inventoryTransaction.setQuantity(salesLine.getQuantitySold());
		inventoryTransaction.setTransactionType(TransactionType.SALE);
		return inventoryTransaction;
	}
    @Query("SELECT coalesce(sum(e.quantity),0 ) from InventoryTransaction e WHERE e.transactionType=?1 AND e.produit.id=?2")
	 Long quantitySold(TransactionType transactionType,Long produitId);
}
