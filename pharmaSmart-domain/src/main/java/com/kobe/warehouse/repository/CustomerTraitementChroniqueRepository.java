package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CustomerTraitementChronique;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerTraitementChroniqueRepository extends JpaRepository<CustomerTraitementChronique, Integer> {
    @EntityGraph(attributePaths = { "dci", "produit" })
    List<CustomerTraitementChronique> findAllByCustomerIdOrderByActifDescCreatedAtAsc(Integer customerId);

    @EntityGraph(attributePaths = { "dci", "produit" })
    List<CustomerTraitementChronique> findAllByActifTrue();
}
