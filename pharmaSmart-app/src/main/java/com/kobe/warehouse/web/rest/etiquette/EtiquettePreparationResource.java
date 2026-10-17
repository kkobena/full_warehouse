package com.kobe.warehouse.web.rest.etiquette;

import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.security.navaccess.NavAccessExempt;
import com.kobe.warehouse.service.dto.etiquette.EtiquettePreparationDTO;
import com.kobe.warehouse.service.etiquette.EtiquetteUsageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Étiquette de préparation magistrale.
 *
 * <p>En POST parce que le contenu est saisi à l'impression : composition, patient, prescripteur et
 * dates n'existent nulle part en base. Rien n'est conservé — l'étiquette est une sortie, pas un
 * enregistrement.
 */
@RestController
@RequestMapping("/api/etiquettes")
public class EtiquettePreparationResource {

    private final EtiquetteUsageService etiquetteUsageService;

    public EtiquettePreparationResource(EtiquetteUsageService etiquetteUsageService) {
        this.etiquetteUsageService = etiquetteUsageService;
    }

    @PostMapping("/preparation")
    @NavAccessExempt("impression d'étiquettes, comme les autres étiquettes de l'officine")
    public ResponseEntity<byte[]> preparation(
        @Valid @RequestBody EtiquettePreparationDTO saisie,
        @RequestParam(defaultValue = "1") int qty,
        @RequestParam(defaultValue = "1") int startAt,
        @RequestParam(required = false) FormatEtiquette format
    ) {
        byte[] pdf = etiquetteUsageService.imprimerPreparation(saisie, qty, startAt, formatOuDefaut(format));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "preparation.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /** La même préparation, en ZPL, pour une imprimante à rouleau pilotée en direct. */
    @PostMapping(value = "/preparation/zpl", produces = MediaType.TEXT_PLAIN_VALUE)
    @NavAccessExempt("impression d'étiquettes, comme les autres étiquettes de l'officine")
    public ResponseEntity<String> preparationZpl(
        @Valid @RequestBody EtiquettePreparationDTO saisie,
        @RequestParam(defaultValue = "1") int qty,
        @RequestParam(required = false) FormatEtiquette format,
        @RequestParam(required = false) ResolutionEtiquette resolution
    ) {
        return ResponseEntity.ok()
            .body(
                etiquetteUsageService.genererZplPreparation(
                    saisie,
                    qty,
                    formatOuDefaut(format),
                    resolution == null ? ResolutionEtiquette.parDefaut() : resolution
                )
            );
    }

    private FormatEtiquette formatOuDefaut(FormatEtiquette format) {
        return format == null ? FormatEtiquette.parDefaut() : format;
    }
}
