package com.kobe.warehouse.web.rest.remise;

import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.remise.JournalRemisesService;
import com.kobe.warehouse.service.remise.dto.JournalRemisesFiltre;
import com.kobe.warehouse.service.remise.dto.LigneJournalDTO;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import com.kobe.warehouse.service.remise.dto.OrigineRemise;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Le journal des remises accordées et des UG vendues (docs/PLAN-JOURNAL-REMISES.md) : le sous-menu de
 * la Gestion Courante, et l'onglet « Journal » du module Promotions. L'un des deux droits suffit.
 */
@RestController
@RequestMapping("/api/journal-remises")
@RequiresNavAccess({"journal-remises", "promotions.journal"})
public class JournalRemisesResource {

    private final JournalRemisesService journalRemisesService;

    public JournalRemisesResource(JournalRemisesService journalRemisesService) {
        this.journalRemisesService = journalRemisesService;
    }

    @GetMapping("/synthese")
    public ResponseEntity<SyntheseJournalDTO> calculerSynthese(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) Set<Integer> caissiers,
            @RequestParam(required = false) Set<Integer> vendeurs,
            @RequestParam(required = false) Set<NatureVente> naturesVente,
            @RequestParam(required = false) Set<Long> promotions,
            @RequestParam(required = false) Set<OrigineRemise> origines,
            @RequestParam(required = false) Set<Integer> familles) {
        return ResponseEntity.ok(journalRemisesService.calculerSynthese(
                new JournalRemisesFiltre(du, au, caissiers, vendeurs, naturesVente, promotions, origines, familles)));
    }

    @GetMapping("/lignes")
    public ResponseEntity<List<LigneJournalDTO>> listerLignes(
            @RequestParam NatureJournal nature,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) Set<Integer> caissiers,
            @RequestParam(required = false) Set<Integer> vendeurs,
            @RequestParam(required = false) Set<NatureVente> naturesVente,
            @RequestParam(required = false) Set<Long> promotions,
            @RequestParam(required = false) Set<OrigineRemise> origines,
            @RequestParam(required = false) Set<Integer> familles,
            Pageable pageable) {
        Page<LigneJournalDTO> page = journalRemisesService.listerLignes(
                nature,
                new JournalRemisesFiltre(du, au, caissiers, vendeurs, naturesVente, promotions, origines, familles),
                pageable);
        HttpHeaders headers =
                PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * Le journal en PDF, en classeur ou en CSV : sans {@code nature}, la synthèse et les trois
     * journaux. Il demande le droit d'export.
     */
    @GetMapping("/export")
    @RequiresNavAccess(
            value = {"journal-remises", "promotions.journal"},
            action = NavAction.EXPORT)
    public ResponseEntity<byte[]> exporter(
            @RequestParam FormatExport format,
            @RequestParam(required = false) NatureJournal nature,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) Set<Integer> caissiers,
            @RequestParam(required = false) Set<Integer> vendeurs,
            @RequestParam(required = false) Set<NatureVente> naturesVente,
            @RequestParam(required = false) Set<Long> promotions,
            @RequestParam(required = false) Set<OrigineRemise> origines,
            @RequestParam(required = false) Set<Integer> familles) {
        JournalRemisesFiltre filtre =
                new JournalRemisesFiltre(du, au, caissiers, vendeurs, naturesVente, promotions, origines, familles);
        byte[] contenu = journalRemisesService.exporter(format, nature, filtre);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(format.getTypeMime()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(journalRemisesService.nommerFichier(format, nature, filtre))
                .build());
        return ResponseEntity.ok().headers(headers).body(contenu);
    }
}
