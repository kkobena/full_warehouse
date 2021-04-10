package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Commande entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
}
