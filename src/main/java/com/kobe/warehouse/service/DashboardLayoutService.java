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
     * Set layout as default for current user
     */
    DashboardLayoutDTO setAsDefault(Integer id);

    /**
     * Delete layout
     */
    void delete(Integer id);

    /**
     * Clone a layout (create a copy for current user)
     */
    DashboardLayoutDTO clone(Integer id, String newName);
}
