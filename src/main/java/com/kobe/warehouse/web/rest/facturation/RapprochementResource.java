package com.kobe.warehouse.web.rest.facturation;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.service.facturation.dto.EtatRapprochementDto;
import com.kobe.warehouse.service.facturation.dto.RapprochementParams;
import com.kobe.warehouse.service.facturation.service.RapprochementService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/rapprochement")
public class RapprochementResource {

    private final RapprochementService rapprochementService;

    public RapprochementResource(RapprochementService rapprochementService) {
        this.rapprochementService = rapprochementService;
    }

    @GetMapping
    public ResponseEntity<List<EtatRapprochementDto>> getEtatRapprochement(
        @RequestParam(required = false) Integer tiersPayantId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Set<InvoiceStatut> statuts,
        Pageable pageable
    ) {
        RapprochementParams params = new RapprochementParams(tiersPayantId, startDate, endDate, statuts);
        Page<EtatRapprochementDto> page = rapprochementService.getEtatRapprochement(params, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
        @RequestParam(required = false) Integer tiersPayantId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Set<InvoiceStatut> statuts
    ) {
        RapprochementParams params = new RapprochementParams(tiersPayantId, startDate, endDate, statuts);
        byte[] pdf = rapprochementService.exportPdf(params);
        return Utils.printPDF(pdf, "rapprochement.pdf");
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
        @RequestParam(required = false) Integer tiersPayantId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Set<InvoiceStatut> statuts
    ) {
        RapprochementParams params = new RapprochementParams(tiersPayantId, startDate, endDate, statuts);
        byte[] excel = rapprochementService.exportExcel(params);
        return Utils.exportExcel(excel, "rapprochement.xlsx");
    }
}
