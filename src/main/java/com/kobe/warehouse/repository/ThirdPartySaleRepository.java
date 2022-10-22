package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ThirdPartySales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThirdPartySaleRepository extends JpaRepository<ThirdPartySales, Long> {
    @Query("select sale from ThirdPartySales sale left join fetch sale.salesLines where sale.id =:id")
    Optional<ThirdPartySales> findOneWithEagerSalesLines(@Param("id") Long id);
}
