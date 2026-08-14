package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.LicenseState;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accès à la ligne singleton {@code license_state}.
 */
@Repository
public interface LicenseStateRepository extends JpaRepository<LicenseState, Integer> {
    default Optional<LicenseState> findSingleton() {
        return findById(LicenseState.SINGLETON_ID);
    }

    /**
     * Rafraîchit la seule sonde anti-recul d'horloge.
     *
     * <p>Écriture ciblée plutôt que {@code save()} d'une entité chargée : ce point est appelé toutes
     * les 15 minutes et ne doit ni charger le jeton complet, ni risquer d'écraser un état concurrent.
     */
    @Modifying
    @Transactional
    @Query("update LicenseState s set s.lastSeenInstant = :instant where s.id = 1")
    int touchLastSeenInstant(Instant instant);
}
