package com.kobe.warehouse.web.rest.facturation;

import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import com.kobe.warehouse.service.facturation.dto.AvoirCommand;
import com.kobe.warehouse.service.facturation.dto.AvoirDto;
import com.kobe.warehouse.service.facturation.dto.AvoirSearchParams;
import com.kobe.warehouse.service.facturation.service.AvoirService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/avoirs")
public class AvoirResource {

    private final AvoirService avoirService;

    public AvoirResource(AvoirService avoirService) {
        this.avoirService = avoirService;
    }

    @GetMapping
    public ResponseEntity<List<AvoirDto>> findAll(
        @RequestParam(required = false) Integer tiersPayantId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) List<AvoirStatut> statuts,
        @RequestParam(required = false) String numAvoir,
        Pageable pageable
    ) {
        AvoirSearchParams params = new AvoirSearchParams(tiersPayantId, startDate, endDate, statuts, numAvoir);
        Page<AvoirDto> page = avoirService.findAll(params, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping
    public ResponseEntity<AvoirDto> creerAvoir(@RequestBody AvoirCommand command) {
        AvoirDto result = avoirService.creerAvoir(command);
        return ResponseEntity.created(URI.create("/api/avoirs/" + result.id())).body(result);
    }

    @PostMapping("/{id}/emettre")
    public ResponseEntity<AvoirDto> emettre(@PathVariable Long id) {
        return ResponseEntity.ok(avoirService.emettre(id));
    }

    @PostMapping("/{id}/imputer")
    public ResponseEntity<Void> imputer(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        Long factureId = body.get("factureId") != null ? Long.valueOf(body.get("factureId").toString()) : null;
        LocalDate factureDate = body.get("factureDate") != null
            ? LocalDate.parse(body.get("factureDate").toString())
            : null;
        avoirService.imputer(id, factureId, factureDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Void> annuler(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String motif = body != null ? body.get("motif") : null;
        avoirService.annuler(id, motif);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annulerLegacy(@PathVariable Long id) {
        avoirService.annuler(id, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        return Utils.printPDF(avoirService.exportPdf(id), "avoir_" + id + ".pdf");
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportListPdf(
        @RequestParam(required = false) Integer tiersPayantId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) List<AvoirStatut> statuts,
        @RequestParam(required = false) String numAvoir
    ) {
        AvoirSearchParams params = new AvoirSearchParams(tiersPayantId, startDate, endDate, statuts, numAvoir);
        return Utils.printPDF(avoirService.exportListPdf(params), "avoirs_" + LocalDate.now() + ".pdf");
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
        @RequestParam(required = false) Integer tiersPayantId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) List<AvoirStatut> statuts,
        @RequestParam(required = false) String numAvoir
    ) {
        AvoirSearchParams params = new AvoirSearchParams(tiersPayantId, startDate, endDate, statuts, numAvoir);
        return Utils.exportExcel(avoirService.exportExcel(params), "avoirs_" + LocalDate.now() + ".xlsx");
    }
}
