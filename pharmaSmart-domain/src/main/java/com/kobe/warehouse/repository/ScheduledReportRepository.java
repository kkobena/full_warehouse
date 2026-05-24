package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ScheduledReport;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for ScheduledReport entity
 */
@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, Integer> {
    /**
     * Find all active scheduled reports
     */
    List<ScheduledReport> findByActiveTrue();

    /**
     * Find scheduled reports due for execution
     */
    @Query("SELECT sr FROM ScheduledReport sr WHERE sr.active = true AND sr.nextExecution <= :now")
    List<ScheduledReport> findDueReports(LocalDateTime now);

    /**
     * Find scheduled reports by created user
     */
    List<ScheduledReport> findByCreatedById(Integer userId);
}
