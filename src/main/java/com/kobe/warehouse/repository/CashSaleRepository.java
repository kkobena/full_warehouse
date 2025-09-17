package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.SaleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashSaleRepository extends JpaRepository<CashSale, SaleId> {
    @Query("select sale from CashSale sale left join fetch sale.salesLines where sale.id =:id AND  sale.saleDate =:saleDate")
    Optional<CashSale> findOneWithEagerSalesLines(@Param("id") Long id, @Param("saleDate") java.time.LocalDate saleDate);
}
