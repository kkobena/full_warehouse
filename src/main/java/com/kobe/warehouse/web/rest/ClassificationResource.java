package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.service.scheduler.ClassificationCriticiteService;
import com.kobe.warehouse.service.dto.ClassificationConfigDTO;
import com.kobe.warehouse.service.dto.ClassificationLogDTO;
import com.kobe.warehouse.service.dto.ClassificationScoreDTO;
import com.kobe.warehouse.service.dto.ReclassificationResultDTO;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing product criticality classification.
 * Provides endpoints for:
 * - Viewing classification metrics and scores
 * - Managing classification configuration
 * - Triggering manual reclassification
 * - Viewing classification history
 * - Overriding product classification
 */
@RestController
@RequestMapping("/api/classification")
public class ClassificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClassificationResource.class);

    private final ClassificationCriticiteService classificationService;

    public ClassificationResource(ClassificationCriticiteService classificationService) {
        this.classificationService = classificationService;
    }

    // ==================== Métriques et Scores ====================

    /**
     * GET /api/classification/metriques/{produitId} : Get classification metrics for a product.
     *
     * @param produitId Product ID
     * @return Classification score with all metrics
     */
    @GetMapping("/metriques/{produitId}")
    public ResponseEntity<ClassificationScoreDTO> getMetriques(@PathVariable Integer produitId) {
        LOG.debug("REST request to get classification metrics for product: {}", produitId);

        ClassificationScoreDTO score = classificationService.calculerScore(produitId);
        if (score == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(score);
    }

    /**
     * GET /api/classification/distribution : Get current class distribution.
     *
     * @return Map of class -> count
     */
    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Long>> getDistribution() {
        LOG.debug("REST request to get class distribution");
        return ResponseEntity.ok(classificationService.getDistributionClasses());
    }

    // ==================== Configuration ====================

    /**
     * GET /api/classification/config : Get classification configuration.
     *
     * @return Current configuration
     */
    @GetMapping("/config")
    public ResponseEntity<ClassificationConfigDTO> getConfig() {
        LOG.debug("REST request to get classification config");
        return ResponseEntity.ok(classificationService.getConfigDTO());
    }

    /**
     * PUT /api/classification/config : Update classification configuration.
     * Admin only.
     *
     * @param config New configuration
     * @return Updated configuration
     */
    @PutMapping("/config")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ClassificationConfigDTO> updateConfig(
        @Valid @RequestBody ClassificationConfigDTO config
    ) {
        LOG.info("REST request to update classification config");

        // Validate thresholds order
        if (!config.isSeuilsValides()) {
            return ResponseEntity.badRequest().build();
        }

        ClassificationConfigDTO updated = classificationService.updateConfig(config);
        return ResponseEntity.ok(updated);
    }

    // ==================== Reclassification ====================

    /**
     * POST /api/classification/recalculate : Force reclassification of all products.
     * Admin only. Use with caution as this can affect many products.
     *
     * @param reason Optional reason for reclassification
     * @return Reclassification result with statistics
     */
    @PostMapping("/recalculate")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ReclassificationResultDTO> forceReclassification(
        @RequestParam(required = false, defaultValue = "Reclassification manuelle admin") String reason
    ) {
        LOG.info("REST request to force reclassification: {}", reason);

        ReclassificationResultDTO result = classificationService.reclassifierTousProduits(reason);

        // Log warning if anomaly detected
        if (result.hasAnomaliePotentielle()) {
            LOG.warn("Reclassification anomaly detected: {}% changes",
                String.format("%.1f", result.getPourcentageChangements()));
        }

        return ResponseEntity.ok(result);
    }

    // ==================== Override ====================

    /**
     * POST /api/classification/{produitId}/override : Override product classification.
     * Admin only. The product will be marked as overridden and won't be auto-reclassified.
     *
     * @param produitId Product ID
     * @param request Override request with new class and reason
     * @return Updated classification score
     */
    @PostMapping("/{produitId}/override")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ClassificationScoreDTO> overrideClasse(
        @PathVariable Integer produitId,
        @RequestBody OverrideClasseRequest request
    ) {
        LOG.info("REST request to override classification for product {}: {} -> {}",
            produitId, request.ancienneClasse(), request.nouvelleClasse());

        if (request.nouvelleClasse() == null) {
            return ResponseEntity.badRequest().build();
        }

        classificationService.overrideClasse(
            produitId,
            request.nouvelleClasse(),
            request.raison() != null ? request.raison() : "Override manuel"
        );

        ClassificationScoreDTO score = classificationService.calculerScore(produitId);
        return ResponseEntity.ok(score);
    }

    /**
     * Request body for override endpoint
     */
    public record OverrideClasseRequest(
        ClasseCriticite ancienneClasse,
        ClasseCriticite nouvelleClasse,
        String raison
    ) {}

    // ==================== Logs ====================

    /**
     * GET /api/classification/logs : Get classification change logs with pagination.
     *
     * @param pageable Pagination
     * @return Page of classification logs
     */
    @GetMapping("/logs")
    public ResponseEntity<List<ClassificationLogDTO>> getLogs(Pageable pageable) {
        LOG.debug("REST request to get classification logs");

        Page<ClassificationLogDTO> page = classificationService.getLogs(pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(),
            page
        );

        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /api/classification/logs/{produitId} : Get classification logs for a product.
     *
     * @param produitId Product ID
     * @param pageable Pagination
     * @return Page of classification logs for the product
     */
    @GetMapping("/logs/{produitId}")
    public ResponseEntity<List<ClassificationLogDTO>> getLogsProduit(
        @PathVariable Integer produitId,
        Pageable pageable
    ) {
        LOG.debug("REST request to get classification logs for product: {}", produitId);

        Page<ClassificationLogDTO> page = classificationService.getLogsProduit(produitId, pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(),
            page
        );

        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
