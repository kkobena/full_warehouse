  package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.cahier_recette.CahierRecettePdfService;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CahierRecetteResource {

    private final Logger log = LoggerFactory.getLogger(CahierRecetteResource.class);

    private final CahierRecettePdfService cahierRecettePdfService;

    public CahierRecetteResource(CahierRecettePdfService cahierRecettePdfService) {
        this.cahierRecettePdfService = cahierRecettePdfService;
    }

    /**
     * {@code GET  /cahier-recette/pdf} : génère le guide des fonctionnalités en PDF
     * (table des matières paginée + bookmarks).
     *
     * @return le PDF en tableau d'octets.
     */
    @GetMapping("/cahier-recette/pdf")
    public ResponseEntity<byte[]> getPdf(
        @RequestParam(name = "modules", required = false) String requestedModules,
        @RequestParam(name = "scenarios", required = false) String requestedScenarios) {
        log.debug("REST request to generate cahier de recette PDF");

        List<String> moduleIds = parseSelection(requestedModules);
        List<String> scenarioIds = parseSelection(requestedScenarios);
        byte[] pdf = cahierRecettePdfService.generatePdf(moduleIds, scenarioIds);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guide-fonctionnalites.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    private List<String> parseSelection(String requestedIds) {
        return requestedIds == null
            ? List.of()
            : Arrays.stream(requestedIds.split(","))
                .map(String::trim)
                .filter(id -> id.matches("[A-Za-z0-9-]{1,16}"))
                .distinct()
                .toList();
    }
}
