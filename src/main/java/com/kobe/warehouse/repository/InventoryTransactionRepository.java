package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.InventoryTransaction_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the InventoryTransaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface InventoryTransactionRepository
    extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {
    List<InventoryTransaction> findByProduitId(Long produitId, Sort sort);

    default InventoryTransaction buildInventoryTransaction(OrderLine orderLine, User user) {
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setCreatedAt(orderLine.getCreatedAt());
        inventoryTransaction.setProduit(orderLine.getFournisseurProduit().getProduit());
        inventoryTransaction.setUser(user);
        inventoryTransaction.setQuantity(orderLine.getQuantityReceived());
        inventoryTransaction.setTransactionType(TransactionType.COMMANDE);
        return inventoryTransaction;
    }

    default InventoryTransaction buildInventoryTransaction(SalesLine salesLine, TransactionType transactionType, User user) {
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setCreatedAt(salesLine.getCreatedAt());
        inventoryTransaction.setProduit(salesLine.getProduit());
        inventoryTransaction.setUser(user);
        inventoryTransaction.setMagasin(user.getMagasin());
        inventoryTransaction.setQuantity(salesLine.getQuantityRequested());
        inventoryTransaction.setCostAmount(salesLine.getCostAmount());
        inventoryTransaction.setRegularUnitPrice(salesLine.getRegularUnitPrice());
        inventoryTransaction.setTransactionType(transactionType);
        inventoryTransaction.setSaleLine(salesLine);
        return inventoryTransaction;
    }

    default InventoryTransaction buildInventoryTransaction(SalesLine salesLine, User user) {
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setCreatedAt(LocalDateTime.now());
        inventoryTransaction.setProduit(salesLine.getProduit());
        inventoryTransaction.setUser(user);
        inventoryTransaction.setMagasin(user.getMagasin());
        inventoryTransaction.setQuantity(salesLine.getQuantityRequested());
        inventoryTransaction.setCostAmount(salesLine.getCostAmount());
        inventoryTransaction.setRegularUnitPrice(salesLine.getRegularUnitPrice());
        inventoryTransaction.setTransactionType(TransactionType.SALE);
        inventoryTransaction.setSaleLine(salesLine);
        return inventoryTransaction;
    }

    @Query("SELECT coalesce(sum(e.quantity),0 ) from InventoryTransaction e WHERE e.transactionType=?1 AND e.produit.id=?2")
    Long quantitySold(TransactionType transactionType, Long produitId);

    default Specification<InventoryTransaction> specialisationProduitId(Long produitId) {
        return (root, query, cb) -> cb.equal(root.get(InventoryTransaction_.produit).get(Produit_.id), produitId);
    }

    default Specification<InventoryTransaction> specialisationDateMvt(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(root.get(InventoryTransaction_.createdAt), startDate, endDate);
    }

    default Specification<InventoryTransaction> specialisationDateGreaterThanOrEqualTo(LocalDateTime startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(InventoryTransaction_.createdAt), startDate);
    }

    default Specification<InventoryTransaction> specialisationDateLessThanOrEqualTo(LocalDateTime endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(InventoryTransaction_.createdAt), endDate);
    }

    default Specification<InventoryTransaction> specialisationTypeTransaction(TransactionType typeTransaction) {
        return (root, query, cb) -> cb.equal(root.get(InventoryTransaction_.transactionType), typeTransaction);
    }
}
