package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.dashboard.widget.AllowedWidgetDTO;
import com.kobe.warehouse.service.dashboard.widget.DashboardWidgetService;
import com.kobe.warehouse.service.dashboard.widget.WidgetData;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Données des widgets du dashboard personnalisable. Les droits sont vérifiés par le service. */
@RestController
@RequestMapping("/api/dashboard-widgets")
public class DashboardWidgetResource {

    private static final Set<String> CONTEXT_PARAMS = Set.of("startDate", "endDate", "magasinId");

    private final DashboardWidgetService dashboardWidgetService;

    public DashboardWidgetResource(DashboardWidgetService dashboardWidgetService) {
        this.dashboardWidgetService = dashboardWidgetService;
    }

    /** GET /api/dashboard-widgets/allowed : widgets que l'utilisateur peut ajouter. */
    @GetMapping("/allowed")
    public ResponseEntity<List<AllowedWidgetDTO>> getAllowed() {
        return ResponseEntity.ok(dashboardWidgetService.findAllowed());
    }

    /** GET /api/dashboard-widgets/{key} : données d'un widget ; les paramètres propres au widget passent en query string. */
    @GetMapping("/{key}")
    public ResponseEntity<WidgetData> load(
        @PathVariable String key,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Integer magasinId,
        @RequestParam Map<String, String> allParams
    ) {
        Map<String, String> params = new HashMap<>(allParams);
        params.keySet().removeAll(CONTEXT_PARAMS);
        return ResponseEntity.ok(dashboardWidgetService.load(key, startDate, endDate, magasinId, params));
    }
}
