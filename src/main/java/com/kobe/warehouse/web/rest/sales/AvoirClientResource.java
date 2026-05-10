package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.service.sale.AvoirClientDocumentService;
import com.kobe.warehouse.service.sale.AvoirClientService;
import com.kobe.warehouse.service.sale.dto.AvoirClientDTO;
import com.kobe.warehouse.service.sale.dto.AvoirClientDocumentDTO;
import com.kobe.warehouse.service.sale.dto.CloturerAvoirRequest;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class AvoirClientResource {

    private final AvoirClientService avoirClientService;
    private final AvoirClientDocumentService avoirClientDocumentService;

    public AvoirClientResource(
        AvoirClientService avoirClientService,
        AvoirClientDocumentService avoirClientDocumentService
    ) {
        this.avoirClientService = avoirClientService;
        this.avoirClientDocumentService = avoirClientDocumentService;
    }

    @GetMapping("/sales/avoirs")
    public ResponseEntity<List<AvoirClientDTO>> getAvoirs(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        Pageable pageable
    ) {
        Page<AvoirClientDTO> page = avoirClientService.findAvoirs(search, fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/sales/avoirs/documents")
    public ResponseEntity<List<AvoirClientDocumentDTO>> getAvoirDocuments(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) AvoirClientStatut statut,
        Pageable pageable
    ) {
        Page<AvoirClientDocumentDTO> page = avoirClientDocumentService.findAll(search, fromDate, toDate, statut, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/sales/avoirs/documents/{id}/cloturer")
    public ResponseEntity<AvoirClientDocumentDTO> cloturerAvoir(
        @PathVariable Integer id,
        @RequestBody CloturerAvoirRequest request
    ) {
        return ResponseEntity.ok(avoirClientDocumentService.cloturerAvoir(id, request));
    }
}
