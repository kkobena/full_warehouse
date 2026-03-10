package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.MargeDTO;
import com.kobe.warehouse.service.dto.report.MargeSummaryDTO;
import com.kobe.warehouse.service.report.MargeReportService;
import com.kobe.warehouse.service.report.pdf.ProfitabilityPdfReportService;
import com.kobe.warehouse.web.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfitabilityReportResource {



    private final MargeReportService margeReportService;

    public ProfitabilityReportResource(


        MargeReportService margeReportService
    ) {


        this.margeReportService = margeReportService;
    }


    @GetMapping(value = "/profitability/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportProfitabilityToPdf(  @RequestParam(required = false) Integer familleProduitId,
                                                             @RequestParam(required = false) String search) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=profitability-report.pdf");
        return ResponseEntity.ok().headers(headers).body(margeReportService.export(familleProduitId, search));
    }

    // =========================================================================
    // Nouveaux endpoints Marges — paginés, sans BCG, basés sur mv_marge_produit
    // =========================================================================

    /**
     * GET /api/marges-profitability
     * Liste paginée des marges avec filtres optionnels famille et recherche textuelle.
     *
     * @param familleProduitId filtre optionnel sur la famille produit
     * @param search           filtre textuel sur libellé ou code CIP
     * @param pageable         pagination + tri (défaut : marge_brute DESC, 20 par page)
     */
    @GetMapping("/marges-profitability")
    public ResponseEntity<List<MargeDTO>> getMarges(
        @RequestParam(required = false) Integer familleProduitId,
        @RequestParam(required = false) String search,
        @PageableDefault(size = 20, sort = "margeBrute") Pageable pageable
    ) {
        Page<MargeDTO> page = margeReportService.getMarges(familleProduitId, search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /api/marges-profitability/faible-marge
     * Produits dont le taux de marge est inférieur au seuil.
     *
     * @param seuil seuil en % (défaut 10)
     */
    @GetMapping("/marges-profitability/faible-marge")
    public ResponseEntity<List<MargeDTO>> getFaibleMarge(
        @RequestParam(defaultValue = "10") int seuil,
        @PageableDefault(size = 20) Pageable pageable
    ) {

        Page<MargeDTO> page = margeReportService.getProduitsMargeInsuffisante(seuil, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /api/marges-profitability/top
     * Top N produits par marge brute décroissante.
     *
     * @param limit nombre de produits (défaut 20)
     */
    @GetMapping("/marges-profitability/top")
    public ResponseEntity<List<MargeDTO>> getTopMarges(
        @RequestParam(defaultValue = "20") int limit,
        @PageableDefault(size = 20) Pageable pageable
    ) {

        Page<MargeDTO> page = margeReportService.getTopProduitsParMarge(limit, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /api/marges-profitability/summary
     * Résumé global des marges avec seuils configurables, sans distribution BCG.
     *
     * @param familleProduitId filtre optionnel famille produit
     * @param seuilBas         seuil bas en % (défaut 10)
     * @param seuilHaut        seuil haut en % (défaut 20)
     */
    @GetMapping("/marges-profitability/summary")
    public ResponseEntity<MargeSummaryDTO> getMargeSummary(
        @RequestParam(required = false) Integer familleProduitId,
        @RequestParam(defaultValue = "10") int seuilBas,
        @RequestParam(defaultValue = "20") int seuilHaut
    ) {
        return ResponseEntity.ok(margeReportService.getMargeSummary(familleProduitId, seuilBas, seuilHaut));
    }
}

