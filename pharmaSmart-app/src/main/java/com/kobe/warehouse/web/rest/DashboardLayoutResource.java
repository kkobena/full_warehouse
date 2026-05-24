package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.DashboardLayoutService;
import com.kobe.warehouse.service.dto.DashboardLayoutDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Dashboard Layouts
 */
@RestController
@RequestMapping("/api/dashboard-layouts")
public class DashboardLayoutResource {

    private final DashboardLayoutService dashboardLayoutService;

    public DashboardLayoutResource(DashboardLayoutService dashboardLayoutService) {
        this.dashboardLayoutService = dashboardLayoutService;
    }

    /**
     * POST /api/dashboard-layouts : Create a new dashboard layout
     */
    @PostMapping("")
    public ResponseEntity<DashboardLayoutDTO> createDashboardLayout(@Valid @RequestBody DashboardLayoutDTO dashboardLayoutDTO) {
        DashboardLayoutDTO result = dashboardLayoutService.save(dashboardLayoutDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * PUT /api/dashboard-layouts/{id} : Update an existing dashboard layout
     */
    @PutMapping("/{id}")
    public ResponseEntity<DashboardLayoutDTO> updateDashboardLayout(
        @PathVariable Integer id,
        @Valid @RequestBody DashboardLayoutDTO dashboardLayoutDTO
    ) {
        dashboardLayoutDTO.setId(id);
        DashboardLayoutDTO result = dashboardLayoutService.update(dashboardLayoutDTO);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/dashboard-layouts : Get all layouts for current user
     */
    @GetMapping("")
    public ResponseEntity<List<DashboardLayoutDTO>> getAllLayouts() {
        List<DashboardLayoutDTO> layouts = dashboardLayoutService.findAllForCurrentUser();
        return ResponseEntity.ok(layouts);
    }

    /**
     * GET /api/dashboard-layouts/public : Get all public layouts
     */
    @GetMapping("/public")
    public ResponseEntity<List<DashboardLayoutDTO>> getPublicLayouts() {

        return ResponseEntity.ok(dashboardLayoutService.findAllPublic());
    }

    /**
     * GET /api/dashboard-layouts/{id} : Get a specific layout
     */
    @GetMapping("/{id}")
    public ResponseEntity<DashboardLayoutDTO> getLayout(@PathVariable Integer id) {
        return dashboardLayoutService.findOne(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/dashboard-layouts/default : Get current user's personal default layout
     */
    @GetMapping("/default")
    public ResponseEntity<DashboardLayoutDTO> getDefaultLayout() {
        return dashboardLayoutService.findDefaultForCurrentUser()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    /**
     * GET /api/dashboard-layouts/resolved : Résout le layout effectif pour l'utilisateur connecté.
     *
     * Priorité :
     *  1. Layout personnel (user isDefault)
     *  2. Layout par rôle  (authority isDefault)
     *  3. 204 No Content   → HomeComponent affiche DefaultDashboard
     *
     * Champs clés de la réponse :
     *  - isRoute=true  → name contient la route Angular (redirection)
     *  - isRoute=false → layoutConfig contient la config GridStack
     */
    @GetMapping("/resolved")
    public ResponseEntity<DashboardLayoutDTO> getResolvedLayout() {
        return dashboardLayoutService.resolveForCurrentUser()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    /**
     * PUT /api/dashboard-layouts/{id}/set-default : Set layout as default for current user
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<DashboardLayoutDTO> setAsDefault(@PathVariable Integer id) {
        return ResponseEntity.ok(dashboardLayoutService.setAsDefault(id));
    }

    /**
     * PUT /api/dashboard-layouts/{id}/set-default-for-role : Set layout as default for a role (admin only)
     */
    @PutMapping("/{id}/set-default-for-role")
    public ResponseEntity<DashboardLayoutDTO> setAsDefaultForAuthority(
        @PathVariable Integer id,
        @RequestParam String authorityName
    ) {
        return ResponseEntity.ok(dashboardLayoutService.setAsDefaultForAuthority(id, authorityName));
    }

    /**
     * GET /api/dashboard-layouts/by-role : Get all layouts for a specific role (admin only)
     */
    @GetMapping("/by-role")
    public ResponseEntity<List<DashboardLayoutDTO>> getLayoutsByRole(@RequestParam String authorityName) {
        return ResponseEntity.ok(dashboardLayoutService.findAllForAuthority(authorityName));
    }

    /**
     * POST /api/dashboard-layouts/{id}/clone : Clone a layout
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<DashboardLayoutDTO> cloneLayout(@PathVariable Integer id, @RequestParam String newName) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dashboardLayoutService.clone(id, newName));
    }

    /**
     * DELETE /api/dashboard-layouts/{id} : Delete a layout
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLayout(@PathVariable Integer id) {
        dashboardLayoutService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
