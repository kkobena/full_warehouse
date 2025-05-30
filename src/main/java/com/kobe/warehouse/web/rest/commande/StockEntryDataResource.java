package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import com.kobe.warehouse.service.stock.StockEntryDataService;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api")
@Transactional
public class StockEntryDataResource {

    private final StockEntryDataService stockEntryDataServicetryService;

    public StockEntryDataResource(StockEntryDataService stockEntryDataServicetryService) {
        this.stockEntryDataServicetryService = stockEntryDataServicetryService;
    }

    @GetMapping("/commandes/data/entree-stock/list")
    public ResponseEntity<List<DeliveryReceiptDTO>> list(@Valid DeliveryReceiptFilterDTO receiptFilter, Pageable pageable) {
        Page<DeliveryReceiptDTO> page = stockEntryDataServicetryService.fetchAllReceipts(receiptFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/commandes/data/entree-stock/{id}")
    public ResponseEntity<DeliveryReceiptDTO> getOne(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(stockEntryDataServicetryService.findOneById(id));
    }

    @GetMapping("/commandes/data/entree-stock/etiquettes/{id}")
    public ResponseEntity<Resource> getPdf(
        @PathVariable Long id,
        @RequestParam(required = false, name = "startAt", defaultValue = "1") Integer startAt,
        HttpServletRequest request
    ) throws IOException {
        final Resource resource = stockEntryDataServicetryService.printEtiquette(id, startAt);
        return Utils.printPDF(resource, request);
    }

    @GetMapping("/commandes/data/entree-stock/pdf/{id}")
    public ResponseEntity<Resource> getPdf(@PathVariable Long id, HttpServletRequest request) throws IOException {
        final Resource resource = stockEntryDataServicetryService.exportToPdf(id);
        return Utils.printPDF(resource, request);
    }
}
