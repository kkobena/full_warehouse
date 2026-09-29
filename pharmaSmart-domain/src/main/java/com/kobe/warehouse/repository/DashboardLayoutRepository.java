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
public interface DashboardLayoutRepository extends JpaRepository<DashboardLayout, Integer> {

    /**
     * Layouts de l'utilisateur et layouts publics des autres utilisateurs. Les layouts système
     * (sans propriétaire, livrés par migration pour l'accueil par rôle) n'y figurent pas.
     */
    @Query(
        "SELECT dl FROM DashboardLayout dl WHERE dl.user = :user OR (dl.scope = 'PUBLIC' AND dl.user IS NOT NULL) ORDER BY dl.updatedAt DESC"
    )
    List<DashboardLayout> findByUserOrPublic(@Param("user") AppUser user);

    /** Layouts publics créés par des utilisateurs, hors layouts système. */
    @Query("SELECT dl FROM DashboardLayout dl WHERE dl.scope = 'PUBLIC' AND dl.user IS NOT NULL ORDER BY dl.updatedAt DESC")
    List<DashboardLayout> findPublicUserLayouts();

    /** Tous les layouts marqués par défaut de l'utilisateur, quelle que soit leur portée. */
    List<DashboardLayout> findAllByUserAndIsDefaultTrue(AppUser user);

    /**
     * Find user's private layouts only
     */
    List<DashboardLayout> findByUserAndScope(AppUser user, DashboardScope scope);

    /**
     * Find user's default layout (niveau 1 — layout personnel)
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
