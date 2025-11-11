package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ajustement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Ajustement entity.
 */
@Repository
public interface AjustementRepository extends JpaRepository<Ajustement, Integer> {
    List<Ajustement> findAllByAjustId(Integer id);

    Optional<Ajustement> findFirstByAjustIdAndProduitId(Integer ajustId, Integer produitId);
}
