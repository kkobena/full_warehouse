package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import com.kobe.warehouse.service.report.TopProductsReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for top products reports
 */
@RestController
@RequestMapping("/api/top-products")
public class TopProductsReportResource {

    private final TopProductsReportService topProductsReportService;

    public TopProductsReportResource(TopProductsReportService topProductsReportService) {
        this.topProductsReportService = topProductsReportService;
    }

    /**
     * GET /api/top-products/by-revenue : Get top N products by revenue for a specific month
     *
     * @param month the month (first day of month)
     * @param limit number of products to return (default: 20)
     * @return ResponseEntity with list of top products ordered by revenue DESC
     */
    @GetMapping("/by-revenue")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\") or hasAuthority(\"" + AuthoritiesConstants.ROLE_RESPONSABLE_COMMANDE + "\")")
    public ResponseEntity<List<TopProductDTO>> getTopProductsByRevenue(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
        @RequestParam(defaultValue = "20") int limit
    ) {
        List<TopProductDTO> result = topProductsReportService.getTopProductsByRevenue(month, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/top-products/by-quantity : Get top N products by quantity sold for a specific month
     *
     * @param month the month (first day of month)
     * @param limit number of products to return (default: 20)
     * @return ResponseEntity with list of top products ordered by quantity DESC
     */
    @GetMapping("/by-quantity")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\") or hasAuthority(\"" + AuthoritiesConstants.ROLE_RESPONSABLE_COMMANDE + "\")")
    public ResponseEntity<List<TopProductDTO>> getTopProductsByQuantity(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
        @RequestParam(defaultValue = "20") int limit
    ) {
        List<TopProductDTO> result = topProductsReportService.getTopProductsByQuantity(month, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/top-products/all : Get all products stats for a specific month
     *
     * @param month the month (first day of month)
     * @return ResponseEntity with list of all products with stats for that month
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\") or hasAuthority(\"" + AuthoritiesConstants.ROLE_RESPONSABLE_COMMANDE + "\")")
    public ResponseEntity<List<TopProductDTO>> getAllProductsForMonth(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        List<TopProductDTO> result = topProductsReportService.getAllProductsForMonth(month);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/top-products/evolution/{produitId} : Get monthly evolution for a specific product
     *
     * @param produitId the product ID
     * @param nbMonths  number of months to retrieve (default: 6, max: 6)
     * @return ResponseEntity with list of monthly stats for this product
     */
    @GetMapping("/evolution/{produitId}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\") or hasAuthority(\"" + AuthoritiesConstants.ROLE_RESPONSABLE_COMMANDE + "\")")
    public ResponseEntity<List<TopProductDTO>> getProductMonthlyEvolution(
        @PathVariable Integer produitId,
        @RequestParam(defaultValue = "6") int nbMonths
    ) {
        List<TopProductDTO> result = topProductsReportService.getProductMonthlyEvolution(produitId, nbMonths);
        return ResponseEntity.ok(result);
    }
}
