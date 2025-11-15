package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.InventoryTransaction_;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.ProductMvtId;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the InventoryTransaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface InventoryTransactionRepository
    extends
        JpaRepository<InventoryTransaction, ProductMvtId>,
        JpaSpecificationExecutor<InventoryTransaction>,
        InventoryTransactionCustomRepository {
    List<InventoryTransaction> findByProduitId(Integer produitId, Sort sort);

    @Query("SELECT coalesce(max(e.createdAt),null) AS updatedAt from InventoryTransaction e WHERE e.mouvementType=?1 AND e.produit.id=?2")
    Optional<LastDateProjection> fetchLastDateByTypeAndProduitId(MouvementProduit type, Integer produitId);

    @Query("SELECT coalesce(sum(e.quantity),0 ) from InventoryTransaction e WHERE e.mouvementType=?1 AND e.produit.id=?2")
    Long quantitySold(TransactionType transactionType, Integer produitId);

    Optional<InventoryTransaction> findInventoryTransactionById(Long id);

    @Query(value = "SELECT get_product_movements_by_period(:produitId,:magasinId,:startDate, :endDate)", nativeQuery = true)
    String fetchMouvementProduit(
        @Param("produitId") Integer produitId,
        @Param("magasinId") Integer magasinId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    default Specification<InventoryTransaction> specialisationProduitId(Integer produitId) {
        return (root, query, cb) -> cb.equal(root.get(InventoryTransaction_.produit).get(Produit_.id), produitId);
    }

    default Specification<InventoryTransaction> specialisationDateMvt(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(root.get(InventoryTransaction_.createdAt), startDate, endDate);
    }

    default Specification<InventoryTransaction> specialisationMvtTransaction(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get(InventoryTransaction_.transactionDate), startDate, endDate);
    }

    default Specification<InventoryTransaction> specialisationDateGreaterThanOrEqualTo(LocalDate startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(InventoryTransaction_.transactionDate), startDate);
    }

    default Specification<InventoryTransaction> specialisationDateLessThanOrEqualTo(LocalDate endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(InventoryTransaction_.transactionDate), endDate);
    }

    default Specification<InventoryTransaction> specialisationTypeTransaction(TransactionType typeTransaction) {
        return (root, query, cb) -> cb.equal(root.get(InventoryTransaction_.mouvementType), typeTransaction);
    }

    default Specification<InventoryTransaction> specialisationMagasinId(Integer magasinId) {
        return (root, query, cb) -> cb.equal(root.get(InventoryTransaction_.magasin).get(Magasin_.id), magasinId);
    }

    default Specification<InventoryTransaction> combineSpecifications(
        Integer magasinId,
        Integer produitId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Specification<InventoryTransaction> specification = specialisationProduitId(produitId);
        if (startDate != null && endDate != null) {
            specification = specification.and(specialisationMvtTransaction(startDate, endDate));
        } else if (startDate != null) {
            specification = specification.and(specialisationDateGreaterThanOrEqualTo(startDate));
        } else if (endDate != null) {
            specification = specification.and(specialisationDateLessThanOrEqualTo(endDate));
        }
        if (magasinId != null) {
            specification = specification.and(specialisationMagasinId(magasinId));
        }

        return specification;
    }
}
