package com.kobe.warehouse.repository;



import com.kobe.warehouse.domain.GammeProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data  repository for the GammeProduit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GammeProduitRepository extends JpaRepository<GammeProduit, Long>,JpaSpecificationExecutor<GammeProduit> {
    Optional<GammeProduit> findFirstByLibelleEquals(String libelle);
}
