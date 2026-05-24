package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.service.dto.report.ForecastSummaryDTO;
import com.kobe.warehouse.service.dto.report.SalesForecastDTO;
import com.kobe.warehouse.service.report.SalesForecastService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Sales Forecasting
 */
@RestController
@RequestMapping("/api/sales-forecast")
public class SalesForecastResource {

    /** Horizon maximum au-delà duquel la fiabilité de la prévision est dégradée. */
    private static final int MAX_MONTHS_AHEAD = 12;
    private static final int DEFAULT_MONTHS_AHEAD = 3;

    private final SalesForecastService salesForecastService;

    public SalesForecastResource(SalesForecastService salesForecastService) {
        this.salesForecastService = salesForecastService;
    }

    /**
     * GET /api/sales-forecast
     * Prévision des ventes sur N mois.
     * monthsAhead est clampé entre 1 et 12 — au-delà de 6, le niveau de confiance
     * retourné dans chaque {@link SalesForecastDTO} est réduit automatiquement.
     */
    @GetMapping
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<SalesForecastDTO>> getForecast(
        @RequestParam(defaultValue = "" + DEFAULT_MONTHS_AHEAD) Integer monthsAhead,
        @RequestParam(defaultValue = "LINEAR_REGRESSION") String method
    ) {
        int clampedMonths = Math.max(1, Math.min(monthsAhead, MAX_MONTHS_AHEAD));
        return ResponseEntity.ok(salesForecastService.getForecast(clampedMonths, method));
    }

    /**
     * GET /api/sales-forecast/summary
     * Résumé statistique : totaux prévisionnels 3M/6M/12M, taux de croissance
     * annuel composé, précision MAPE, MAE en FCFA, saisonnalité.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ForecastSummaryDTO> getForecastSummary() {
        return ResponseEntity.ok(salesForecastService.getForecastSummary());
    }

    /**
     * GET /api/sales-forecast/historical
     * Données historiques réelles + prévision sur monthsAhead mois à partir de endDate.
     */
    @GetMapping("/historical")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<SalesForecastDTO>> getHistoricalVsForecast(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "" + DEFAULT_MONTHS_AHEAD) Integer monthsAhead
    ) {
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().build();
        }
        int clampedMonths = Math.max(1, Math.min(monthsAhead, MAX_MONTHS_AHEAD));
        return ResponseEntity.ok(salesForecastService.getHistoricalVsForecast(startDate, endDate, clampedMonths));
    }

    /**
     * GET /api/sales-forecast/seasonality
     * Détection de saisonnalité via le ratio η² (variance inter-mois / variance totale).
     * Retourne true si η² > 0.5 sur les 24 derniers mois.
     */
    @GetMapping("/seasonality")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Boolean> detectSeasonality() {
        return ResponseEntity.ok(salesForecastService.detectSeasonality());
    }
}
