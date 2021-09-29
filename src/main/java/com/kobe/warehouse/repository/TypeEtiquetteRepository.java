package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.TypeEtiquette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data  repository for the TypeEtiquette entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TypeEtiquetteRepository extends JpaRepository<TypeEtiquette, Long> {
    Optional<TypeEtiquette> findFirstByLibelleEquals(String libelle);
}
