package com.kobe.warehouse.repository;


import com.kobe.warehouse.domain.FormProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data  repository for the FormProduit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FormProduitRepository extends JpaRepository<FormProduit, Long> {
    Optional<FormProduit> findFirstByLibelleEquals(String libelle);
}
