package com.kobe.warehouse.web.rest.nav;

import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.nav.NavAssignDTO;
import com.kobe.warehouse.service.dto.nav.NavNodeDTO;
import com.kobe.warehouse.service.dto.nav.NavReorderDTO;
import com.kobe.warehouse.service.nav.NavItemService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller pour la navigation dynamique.
 */
@RestController
@RequestMapping("/api")
public class NavItemResource {

    private static final Logger log = LoggerFactory.getLogger(NavItemResource.class);

    private final NavItemService navItemService;
    private final UserService userService;


    public NavItemResource(NavItemService navItemService, UserService userService) {
        this.navItemService = navItemService;
        this.userService = userService;
    }

    /**
     * GET /api/nav/my-items : retourne l'arbre de navigation de l'utilisateur courant.
     */
    @GetMapping("/nav/my-items")
    public ResponseEntity<List<NavNodeDTO>> getMyNavItems() {
        log.debug("REST request to get nav items for current user");
        Set<String> roles = userService.getUserWithAuthorities()
            .map(u -> u.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet()))
            .orElse(Set.of());
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow();
        return ResponseEntity.ok(navItemService.buildTreeForRoles(roles, login));
    }

    /**
     * PUT /api/nav/reorder : sauvegarde l'ordre personnalisé de l'utilisateur courant.
     */
    @PutMapping("/nav/reorder")
    public ResponseEntity<Void> reorderUserItems(@Valid @RequestBody List<NavReorderDTO> reorderList) {
        log.debug("REST request to save user nav reorder");
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow();
        navItemService.saveUserOrder(login, reorderList);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/admin/nav/items : retourne tous les items (vue admin).
     * Si roleName est fourni, les permissions sont pré-chargées pour ce rôle.
     */
    @GetMapping("/admin/nav/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NavNodeDTO>> getAllNavItems(
        @RequestParam(value = "roleName", required = false) String roleName
    ) {
        log.debug("REST request to get all nav items (admin), roleName={}", roleName);
        if (roleName != null && !roleName.isBlank()) {
            return ResponseEntity.ok(navItemService.findAllItemsForRole(roleName));
        }
        return ResponseEntity.ok(navItemService.findAllItems());
    }

    /**
     * PUT /api/admin/nav/reorder : réorganise l'ordre global (admin, partagé pour tous).
     */
    @PutMapping("/admin/nav/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminReorderItems(@Valid @RequestBody List<NavReorderDTO> reorderList) {
        log.debug("REST request to admin reorder nav items");
        navItemService.saveAdminOrder(reorderList);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/admin/nav/assign : assigne des NavItems à un rôle avec des permissions fines.
     */
    @PostMapping("/admin/nav/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignItemsToRole(@Valid @RequestBody NavAssignDTO dto) {
        log.debug("REST request to assign nav items to role: {}", dto.roleName());
        navItemService.assignItemsToRole(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /api/admin/nav/items/{id}/libelle : met à jour le libellé d'un item.
     */
    @PatchMapping("/admin/nav/items/{id}/libelle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateNavItemLibelle(
        @PathVariable Integer id,
        @RequestBody UpdateLibelleRequest request
    ) {
        if (request.libelle() == null || request.libelle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.debug("REST request to update libelle of NavItem {}: {}", id, request.libelle());
        navItemService.updateLibelle(id, request.libelle());
        return ResponseEntity.ok().build();
    }
}

record UpdateLibelleRequest(String libelle) {}

