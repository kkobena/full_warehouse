package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.VenteDepot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VenteDepotRepository extends JpaRepository<VenteDepot, SaleId> {
    @Query("select sale from VenteDepot sale left join fetch sale.salesLines where sale.id =:id AND  sale.saleDate =:saleDate")
    Optional<VenteDepot> findOneWithEagerSalesLines(@Param("id") Long id, @Param("saleDate") java.time.LocalDate saleDate);
}
