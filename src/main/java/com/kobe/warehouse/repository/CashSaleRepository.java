package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashSale;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CashSaleRepository extends JpaRepository<CashSale, Long> {
    @Query("select sale from CashSale sale left join fetch sale.salesLines where sale.id =:id")
    Optional<CashSale> findOneWithEagerSalesLines(@Param("id") Long id);
}
