package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Commande entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    Optional<Commande> getFirstByOrderRefernce(String orderRefernce);
}
