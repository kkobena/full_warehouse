package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Tva;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Tva entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TvaRepository extends JpaRepository<Tva, Long> {
    Optional<Tva> findFirstByTauxEquals(Integer taux);
}
