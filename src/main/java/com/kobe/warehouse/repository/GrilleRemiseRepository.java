package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.GrilleRemise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the GrilleRemise entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GrilleRemiseRepository extends JpaRepository<GrilleRemise, Long> {}
