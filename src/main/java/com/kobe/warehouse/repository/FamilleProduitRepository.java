package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FamilleProduit;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the FamilleProduit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FamilleProduitRepository extends JpaRepository<FamilleProduit, Long>, JpaSpecificationExecutor<FamilleProduit> {
    Page<FamilleProduit> findAllByCodeOrLibelleContainingAllIgnoreCase(String code, String libelle, Pageable pageable);

    Optional<FamilleProduit> findFirstByLibelleEquals(String libelle);
Pageable
    FamilleProduit findByCodeEquals(String code);
}
