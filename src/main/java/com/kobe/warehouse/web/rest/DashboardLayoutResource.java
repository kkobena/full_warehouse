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
        @PathVariable Long id,
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
        List<DashboardLayoutDTO> layouts = dashboardLayoutService.findAllPublic();
        return ResponseEntity.ok(layouts);
    }

    /**
     * GET /api/dashboard-layouts/{id} : Get a specific layout
     */
    @GetMapping("/{id}")
    public ResponseEntity<DashboardLayoutDTO> getLayout(@PathVariable Long id) {
        return dashboardLayoutService.findOne(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/dashboard-layouts/default : Get current user's default layout
     */
    @GetMapping("/default")
    public ResponseEntity<DashboardLayoutDTO> getDefaultLayout() {
        return dashboardLayoutService.findDefaultForCurrentUser().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    /**
     * PUT /api/dashboard-layouts/{id}/set-default : Set layout as default
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<DashboardLayoutDTO> setAsDefault(@PathVariable Long id) {
        DashboardLayoutDTO result = dashboardLayoutService.setAsDefault(id);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/dashboard-layouts/{id}/clone : Clone a layout
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<DashboardLayoutDTO> cloneLayout(@PathVariable Long id, @RequestParam String newName) {
        DashboardLayoutDTO result = dashboardLayoutService.clone(id, newName);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * DELETE /api/dashboard-layouts/{id} : Delete a layout
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLayout(@PathVariable Long id) {
        dashboardLayoutService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
