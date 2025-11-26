package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.DashboardLayout;
import com.kobe.warehouse.domain.enumeration.DashboardScope;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Dashboard Layout
 */
@Repository
public interface DashboardLayoutRepository extends JpaRepository<DashboardLayout, Long> {
    /**
     * Find all layouts for a specific user (private + public)
     */
    @Query("SELECT dl FROM DashboardLayout dl WHERE dl.user = :user OR dl.scope = 'PUBLIC' ORDER BY dl.updatedAt DESC")
    List<DashboardLayout> findByUserOrPublic(@Param("user") AppUser user);

    /**
     * Find user's private layouts only
     */
    List<DashboardLayout> findByUserAndScope(AppUser user, DashboardScope scope);

    /**
     * Find user's default layout
     */
    Optional<DashboardLayout> findByUserAndIsDefaultTrue(AppUser user);

    /**
     * Find all public layouts
     */
    List<DashboardLayout> findByScope(DashboardScope scope);

    /**
     * Find by user and name
     */
    Optional<DashboardLayout> findByUserAndName(AppUser user, String name);
}
