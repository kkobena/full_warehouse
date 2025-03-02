package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FormProduit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the FormProduit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FormProduitRepository extends JpaRepository<FormProduit, Long> {
    Optional<FormProduit> findFirstByLibelleEquals(String libelle);
}
