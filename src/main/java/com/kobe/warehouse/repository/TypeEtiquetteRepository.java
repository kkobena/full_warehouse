package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.TypeEtiquette;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the TypeEtiquette entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TypeEtiquetteRepository extends JpaRepository<TypeEtiquette, Long> {
    Optional<TypeEtiquette> findFirstByLibelleEquals(String libelle);
}
