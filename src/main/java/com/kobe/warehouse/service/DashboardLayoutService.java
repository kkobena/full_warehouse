package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.DashboardLayoutDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing Dashboard Layouts
 */
public interface DashboardLayoutService {

    /**
     * Save a dashboard layout
     */
    DashboardLayoutDTO save(DashboardLayoutDTO dashboardLayoutDTO);

    /**
     * Update a dashboard layout
     */
    DashboardLayoutDTO update(DashboardLayoutDTO dashboardLayoutDTO);

    /**
     * Get all layouts for current user
     */
    List<DashboardLayoutDTO> findAllForCurrentUser();

    /**
     * Get all public layouts
     */
    List<DashboardLayoutDTO> findAllPublic();

    /**
     * Get layout by ID
     */
    Optional<DashboardLayoutDTO> findOne(Integer id);

    /**
     * Get current user's default layout
     */
    Optional<DashboardLayoutDTO> findDefaultForCurrentUser();

    /**
     * Résout le layout effectif pour l'utilisateur courant.
     *
     * Ordre de priorité :
     *   1. Layout personnel (user_id = currentUser, is_default = true)
     *   2. Layout par rôle  (authority_name = rôle prioritaire, is_default = true)
     *   3. Empty Optional   → HomeComponent affiche DefaultDashboard
     *
     * Si le layout résolu a isRoute = true, name contient la route Angular.
     * Si isRoute = false, layoutConfig contient la configuration GridStack.
     */
    Optional<DashboardLayoutDTO> resolveForCurrentUser();

    /**
     * Set layout as default for current user
     */
    DashboardLayoutDTO setAsDefault(Integer id);

    /**
     * Set layout as default for a role (admin only)
     */
    DashboardLayoutDTO setAsDefaultForAuthority(Integer id, String authorityName);

    /**
     * Delete layout
     */
    void delete(Integer id);

    /**
     * Clone a layout (create a copy for current user)
     */
    DashboardLayoutDTO clone(Integer id, String newName);

    /**
     * Get all layouts for a specific role (admin management)
     */
    List<DashboardLayoutDTO> findAllForAuthority(String authorityName);
}
