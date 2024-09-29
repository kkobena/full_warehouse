package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ImportationEchoueLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the ImportationEchoueLigne entity.
 */
@Repository
public interface ImportationEchoueLigneRepository extends JpaRepository<ImportationEchoueLigne, Long> {}
