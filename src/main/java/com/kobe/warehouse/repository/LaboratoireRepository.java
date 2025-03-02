package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Laboratoire;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Laboratoire entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LaboratoireRepository extends JpaRepository<Laboratoire, Long>, JpaSpecificationExecutor<Laboratoire> {
    Optional<Laboratoire> findFirstByLibelleEquals(String libelle);
}
