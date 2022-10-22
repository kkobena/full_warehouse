package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Reference;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Reference entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReferenceRepository extends JpaRepository<Reference, Long> {
	Optional<Reference> findOneBymvtDateAndType(LocalDate localDate, Integer dType);
}
