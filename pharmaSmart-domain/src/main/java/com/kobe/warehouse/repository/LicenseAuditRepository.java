package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.LicenseAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Journal append-only des événements de licence.
 */
@Repository
public interface LicenseAuditRepository extends JpaRepository<LicenseAudit, Long> {
    Page<LicenseAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
