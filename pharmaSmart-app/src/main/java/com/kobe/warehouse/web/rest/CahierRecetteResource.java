package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.cahier_recette.CahierRecettePdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<byte[]> getPdf() {
        log.debug("REST request to generate cahier de recette PDF");

        byte[] pdf = cahierRecettePdfService.generatePdf();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guide-fonctionnalites.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
