package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ajust;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Ajust entity.
 */
@Repository
public interface AjustRepository extends JpaRepository<Ajust, Long> {
}
