package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data  repository for the Ajustement entity.
 */
@Repository
public interface AjustementRepository extends JpaRepository<Ajustement, Long> {
    List<Ajustement> findAllByAjustId(Long id);

    Optional<Ajustement> findFirstByAjustIdAndProduitId(Long ajustId, Long produitId);

}
