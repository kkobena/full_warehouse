package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.domain.SemoisClasseConfig;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.service.dto.ReapproDashboardDTO;
import com.kobe.warehouse.service.dto.SemoisCommanderDTO;
import com.kobe.warehouse.service.dto.SemoisExclusionDTO;
import com.kobe.warehouse.service.dto.SemoisSuggestionDTO;
import com.kobe.warehouse.service.scheduler.SemoisCalculationService;
import com.kobe.warehouse.service.scheduler.VentesAgregeesService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * REST controller for managing SEMOIS (Stock Économique Mensuel d'Objectif Interne de Sécurité).
 * Provides endpoints for:
 * - Retrieving replenishment suggestions
 * - Managing SEMOIS configurations
 * - Triggering manual recalculations (admin)
 * - Importing historical data (admin)
 */
@RestController
@RequestMapping("/api/semois")
public class SemoisResource {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisResource.class);

    private final SemoisCalculationService semoisCalculationService;
    private final VentesAgregeesService ventesAgregeesService;
    private final SuggestionProduitService suggestionProduitService;

    public SemoisResource(
        SemoisCalculationService semoisCalculationService,
        VentesAgregeesService ventesAgregeesService,
        SuggestionProduitService suggestionProduitService
    ) {
        this.semoisCalculationService = semoisCalculationService;
        this.ventesAgregeesService = ventesAgregeesService;
        this.suggestionProduitService = suggestionProduitService;
    }

    /**
     * GET /api/semois/classe-configs : Récupère les 5 configurations de classe de criticité.
     */
    @GetMapping("/classe-configs")
    public ResponseEntity<List<SemoisClasseConfig>> getClasseConfigs() {
        return ResponseEntity.ok(semoisCalculationService.getAllClasseConfigs());
    }

    /**
     * PUT /api/semois/classe-configs/{classeCriticite} : Met à jour la configuration d'une classe.
     */
    @PutMapping("/classe-configs/{classeCriticite}")
    public ResponseEntity<SemoisClasseConfig> updateClasseConfig(
        @PathVariable ClasseCriticite classeCriticite,
        @Valid @RequestBody SemoisClasseConfig config
    ) {
        LOG.debug("REST request to update SemoisClasseConfig for classe: {}", classeCriticite);
        return semoisCalculationService.updateClasseConfig(classeCriticite, config)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/semois/suggestions : Get all SEMOIS replenishment suggestions with pagination.
     * Supports optional filtering by search text and criticality class.
     *
     * @param pageable Pagination information
     * @param search Text search (product label or CIP code, optional)
     * @param classeCriticite Filter by criticality class (optional)
     * @return Page of SEMOIS suggestions sorted by order quantity descending
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<SemoisSuggestionDTO>> getAllSuggestions(
        Pageable pageable,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) ClasseCriticite classeCriticite,
        @RequestParam(required = false) Integer fournisseurId,
        @RequestParam(required = false) String niveauUrgence
    ) {
        LOG.debug("REST request to get paged SEMOIS suggestions - page: {}, search: {}, classe: {}, fournisseur: {}, urgence: {}",
            pageable.getPageNumber(), search, classeCriticite, fournisseurId, niveauUrgence);

        Page<SemoisSuggestionDTO> page = semoisCalculationService.getAllSuggestions(
            search, classeCriticite, fournisseurId, niveauUrgence, pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);

        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /api/semois/suggestions/urgents : Tous les produits en rupture (sans pagination).
     * Utilisé pour "Commander tous les urgents".
     */
    @GetMapping("/suggestions/urgents")
    public ResponseEntity<List<SemoisSuggestionDTO>> getAllUrgentSuggestions() {
        LOG.debug("REST request to get all urgent SEMOIS suggestions");
        return ResponseEntity.ok(semoisCalculationService.getAllUrgentSuggestions());
    }

    /**
     * GET /api/semois/suggestions/fournisseurs : Fournisseurs distincts ayant des produits SEMOIS.
     */
    @GetMapping("/suggestions/fournisseurs")
    public ResponseEntity<List<java.util.Map<String, Object>>> getSemoisFournisseurs() {
        return ResponseEntity.ok(semoisCalculationService.getDistinctFournisseurs());
    }

    /**
     * GET /api/semois/suggestions/{produitId} : Get SEMOIS suggestion for a specific product.
     *
     * @param produitId Product ID
     * @return SEMOIS suggestion with calculated VMM, safety margin, and order quantity
     */
    @GetMapping("/suggestions/{produitId}")
    public ResponseEntity<SemoisSuggestionDTO> getSuggestionForProduct(@PathVariable Integer produitId) {
        LOG.debug("REST request to get SEMOIS suggestion for product: {}", produitId);
        try {
            SemoisSuggestionDTO suggestion = semoisCalculationService.getSuggestionForProduct(produitId);
            return ResponseEntity.ok().body(suggestion);
        } catch (IllegalArgumentException e) {
            LOG.warn("SEMOIS configuration not found for product {}", produitId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/semois/configuration/{produitId} : Get SEMOIS configuration for a product.
     *
     * @param produitId Product ID
     * @return SEMOIS configuration
     */
    @GetMapping("/configuration/{produitId}")
    public ResponseEntity<SemoisConfiguration> getConfiguration(@PathVariable Integer produitId) {
        LOG.debug("REST request to get SEMOIS configuration for product: {}", produitId);
        return semoisCalculationService.findConfiguration(produitId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /api/semois/configuration : Initialize SEMOIS configuration for a product.
     * If configuration already exists, returns existing one.
     *
     * @param request Request containing produitId and optional classe criticite
     * @return Created or existing SEMOIS configuration
     */
    @PostMapping("/configuration")
    public ResponseEntity<SemoisConfiguration> initializeConfiguration(
        @Valid @RequestBody InitConfigurationRequest request
    ) {
        LOG.debug("REST request to initialize SEMOIS configuration for product: {}", request.produitId);

        SemoisConfiguration config = request.classeCriticite != null
            ? semoisCalculationService.initializeConfiguration(request.produitId, request.classeCriticite)
            : semoisCalculationService.initializeConfiguration(request.produitId);

        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }

    /**
     * PUT /api/semois/configuration/{produitId} : Update SEMOIS configuration.
     *
     * @param produitId Product ID
     * @param config Updated configuration
     * @return Updated configuration
     */
    @PutMapping("/configuration/{produitId}")
    public ResponseEntity<SemoisConfiguration> updateConfiguration(
        @PathVariable Integer produitId,
        @Valid @RequestBody SemoisConfiguration config
    ) {
        LOG.debug("REST request to update SEMOIS configuration for product: {}", produitId);
        if (!semoisCalculationService.configurationExists(produitId)) {
            return ResponseEntity.notFound().build();
        }
        config.getProduit().setId(produitId);
        SemoisConfiguration updated = semoisCalculationService.saveConfiguration(config);
        return ResponseEntity.ok().body(updated);
    }



    /**
     * GET /api/semois/freshness : Get SEMOIS calculation freshness status.
     */
    @GetMapping("/freshness")
    public ResponseEntity<FraicheurResponse> getFreshness() {
        LocalDateTime lastCalc = semoisCalculationService.getLastCalculDate();
        long nbProduits = semoisCalculationService.countConfiguredProducts();
        boolean recente = lastCalc != null && lastCalc.isAfter(LocalDateTime.now().minusHours(24));
        return ResponseEntity.ok(new FraicheurResponse(lastCalc, recente, nbProduits));
    }

    /**
     * GET /api/semois/dashboard : Tableau de bord réapprovisionnement temps réel (Axe 6).
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ReapproDashboardDTO> getDashboard() {
        LOG.debug("REST request to get SEMOIS reappro dashboard");
        ReapproDashboardDTO dashboard = semoisCalculationService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * POST /api/semois/commander : Crée des commandes groupées par fournisseur
     * depuis une liste de suggestions SEMOIS sélectionnées.
     *
     * @deprecated v12 — Le flux SEMOIS passe maintenant par le panier {@link com.kobe.warehouse.domain.Suggestion}.
     * Utiliser {@code POST /api/suggestions/{id}/commander} à la place.
     */
    @Deprecated(since = "2026-03", forRemoval = false)
    @SuppressWarnings("deprecation")
    @PostMapping("/commander")
    public ResponseEntity<Void> commanderSemois(@Valid @RequestBody SemoisCommanderDTO dto) {
        LOG.info("REST request to create commandes from {} SEMOIS suggestions", dto.lignes().size());
        suggestionProduitService.createCommandesFromSemois(dto.lignes());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/semois/recalculate : Déclenche un recalcul SEMOIS forcé pour tous les produits.
     * <p>
     * Contrairement au recalcul planifié (cron nocturne), cet endpoint <strong>ignore la garde
     * journalière</strong> et force un recalcul immédiat même si un recalcul a déjà eu lieu
     * aujourd'hui. Utile après une modification manuelle de configurations ou de classifications.
     * </p>
     * Retourne 409 Conflict si un recalcul est déjà en cours.
     */
    @PostMapping("/recalculate")
    public ResponseEntity<MessageResponse> triggerRecalculation() {
        LOG.info("REST request to force SEMOIS recalculation (bypasses daily guard)");
        try {
            semoisCalculationService.forceRecalculate();
        } catch (IllegalStateException e) {
            LOG.warn("SEMOIS recalculation rejected: already in progress");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageResponse("Un recalcul SEMOIS est déjà en cours. Veuillez réessayer dans quelques minutes."));
        }
        return ResponseEntity.ok().body(new MessageResponse("Recalcul SEMOIS forcé déclenché avec succès"));
    }

    /**
     * POST /api/semois/import-historical : Import historical sales data for N months.
     * Admin only. Should be executed once during initial deployment.
     *
     * @param request Request containing number of months to import
     * @return Success message
     */
    @PostMapping("/import-historical")
    public ResponseEntity<MessageResponse> importHistoricalData(@Valid @RequestBody ImportHistoricalRequest request) {
        LOG.info("REST request to import {} months of historical sales data", request.nbMois);
        ventesAgregeesService.importHistoricalMonths(request.nbMois);
        return ResponseEntity.ok().body(
            new MessageResponse(request.nbMois + " mois de données historiques importés avec succès")
        );
    }

    /**
     * GET /api/semois/aggregation/status : Get aggregation status for current and previous month.
     *
     * @return Aggregation counts and freeze status
     */
    @GetMapping("/aggregation/status")
    public ResponseEntity<AggregationStatusResponse> getAggregationStatus() {
        LOG.debug("REST request to get aggregation status");

        YearMonth now = YearMonth.now();
        YearMonth lastMonth = now.minusMonths(1);

        long currentMonthCount = ventesAgregeesService.countAgregationsForMonth(now);
        long lastMonthCount = ventesAgregeesService.countAgregationsForMonth(lastMonth);
        boolean lastMonthFrozen = ventesAgregeesService.isMonthFrozen(lastMonth);

        return ResponseEntity.ok().body(new AggregationStatusResponse(
            now.toString(),
            currentMonthCount,
            lastMonth.toString(),
            lastMonthCount,
            lastMonthFrozen,
            ventesAgregeesService.getFreezeDelayDays()
        ));
    }

    /**
     * POST /api/semois/aggregation/unfreeze : Unfreeze a month for exceptional corrections.
     * Admin only. Use with caution - requires documented reason.
     *
     * @param request Request containing month and reason
     * @return Success message
     */
    @PostMapping("/aggregation/unfreeze")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> unfreezeMonth(@Valid @RequestBody UnfreezeMonthRequest request) {
        LOG.warn("REST request to unfreeze month {} - Reason: {}", request.anneeMois, request.reason);

        YearMonth mois = YearMonth.parse(request.anneeMois);
        ventesAgregeesService.unfreezeMonth(mois, request.reason);

        return ResponseEntity.ok().body(
            new MessageResponse("Mois " + request.anneeMois + " dégelé. Raison: " + request.reason)
        );
    }

    // ── S4.3 — Exclusion temporaire ──────────────────────────────────────────

    /**
     * POST /api/semois/configuration/{produitId}/exclure
     * Exclut temporairement un produit des suggestions SEMOIS.
     */
    @PostMapping("/configuration/{produitId}/exclure")
    public ResponseEntity<Void> exclureProduit(
        @PathVariable Integer produitId,
        @Valid @RequestBody ExclusionRequest request
    ) {
        LOG.info("REST request to exclude produit {} from SEMOIS for {} days - motif: {}",
            produitId, request.dureeJours, request.motif);
        try {
            semoisCalculationService.exclureProduit(produitId, request.dureeJours, request.motif);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/semois/configuration/{produitId}/exclusion
     * Lève manuellement l'exclusion d'un produit (réintégration immédiate).
     */
    @DeleteMapping("/configuration/{produitId}/exclusion")
    public ResponseEntity<Void> leverExclusion(@PathVariable Integer produitId) {
        LOG.info("REST request to lift exclusion for produit {}", produitId);
        semoisCalculationService.leverExclusion(produitId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/semois/exclusions : Liste toutes les exclusions actuellement actives.
     */
    @GetMapping("/exclusions")
    public ResponseEntity<List<SemoisExclusionDTO>> getExclusionsActives() {
        LOG.debug("REST request to get active SEMOIS exclusions");
        return ResponseEntity.ok(semoisCalculationService.getExclusionsActives());
    }

    /**
     * GET /api/semois/exclusions/count : Nombre d'exclusions actives (pour badge UI).
     */
    @GetMapping("/exclusions/count")
    public ResponseEntity<ExclusionCountResponse> countExclusionsActives() {
        return ResponseEntity.ok(new ExclusionCountResponse(semoisCalculationService.countExclusionsActives()));
    }

    // ==================== DTOs pour les requêtes ====================

    /**
     * Request DTO for initializing SEMOIS configuration
     */
    public record InitConfigurationRequest(
        @NotNull Integer produitId,
        ClasseCriticite classeCriticite
    ) {}

    /** Request DTO for temporarily excluding a product from SEMOIS suggestions */
    public record ExclusionRequest(
        Integer dureeJours,
        String motif
    ) {}

    /** Response DTO for exclusion count badge */
    public record ExclusionCountResponse(long count) {}

    /**
     * Request DTO for importing historical data
     */
    public record ImportHistoricalRequest(
        @NotNull Integer nbMois
    ) {}

    /**
     * Request DTO for unfreezing a month
     */
    public record UnfreezeMonthRequest(
        @NotNull String anneeMois,
        @NotNull String reason
    ) {}


    /**
     * Response DTO for generic messages
     */
    public record MessageResponse(String message) {}

    /**
     * Response DTO for SEMOIS freshness status
     */
    public record FraicheurResponse(
        LocalDateTime dernierCalcul,
        boolean calculeRecent,
        long nbProduitsConfigures
    ) {}

    /**
     * Response DTO for aggregation status
     */
    public record AggregationStatusResponse(
        String currentMonth,
        long currentMonthProductCount,
        String lastMonth,
        long lastMonthProductCount,
        boolean lastMonthFrozen,
        int freezeDelayDays
    ) {}
}
