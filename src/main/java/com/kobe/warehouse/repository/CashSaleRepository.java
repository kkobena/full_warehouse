package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.CashSale_;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.SaleId;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public interface CashSaleRepository extends JpaRepository<CashSale, SaleId> , JpaSpecificationExecutor<CashSale> {
    @Query("select sale from CashSale sale left join fetch sale.salesLines where sale.id =:id AND  sale.saleDate =:saleDate")
    Optional<CashSale> findOneWithEagerSalesLines(@Param("id") Long id, @Param("saleDate") java.time.LocalDate saleDate);

    default Specification<CashSale> between(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> cb.between(root.get(CashSale_.saleDate), fromDate, toDate);
    }
    default Specification<CashSale> filterNumberTransaction(String numberTransaction) {
        if (!StringUtils.hasLength(numberTransaction)) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get(CashSale_.numberTransaction), numberTransaction + "%");
    }
    default Specification<CashSale> hasStatut(EnumSet<SalesStatut> statut) {
        return (root, query, cb) -> root.get(CashSale_.statut).in(statut);
    }
    default Specification<CashSale> hasCaissier(AppUser caissier) {
        return (root, query, cb) -> cb.equal(root.get(CashSale_.caissier), caissier);
    }default Specification<CashSale> belongsToMagasin(Integer magasinId) {
        return (root, query, cb) -> cb.equal(root.get(CashSale_.magasin).get(Magasin_.id), magasinId);
    }

}
