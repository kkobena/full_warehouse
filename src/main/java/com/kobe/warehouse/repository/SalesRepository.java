package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.SalesStatut;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Sales entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalesRepository extends JpaRepository<Sales, Long> {
	  @Query("select sale from Sales sale left join fetch sale.salesLines where sale.id =:id")
	    Optional<Sales> findOneWithEagerSalesLines(@Param("id") Long id);
    
}
