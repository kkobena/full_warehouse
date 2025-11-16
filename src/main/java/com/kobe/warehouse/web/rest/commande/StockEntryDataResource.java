package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptItemProjection;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptProjection;
import com.kobe.warehouse.service.stock.StockEntryDataService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class StockEntryDataResource {

    private final StockEntryDataService stockEntryDataServicetryService;

    public StockEntryDataResource(StockEntryDataService stockEntryDataServicetryService) {
        this.stockEntryDataServicetryService = stockEntryDataServicetryService;
    }

    @GetMapping("/commandes/data/entree-stock/list")
    public ResponseEntity<List<DeliveryReceiptDTO>> fetch(@Valid DeliveryReceiptFilterDTO receiptFilter, Pageable pageable) {
        Page<DeliveryReceiptDTO> page = stockEntryDataServicetryService.fetchAllReceipts(receiptFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/commandes/data/entree-stock/{id}/{orderDate}")
    public ResponseEntity<DeliveryReceiptDTO> getOne(@PathVariable Integer id, @PathVariable LocalDate orderDate) {
        return ResponseUtil.wrapOrNotFound(stockEntryDataServicetryService.findOneById(new CommandeId(id, orderDate)));
    }

    @GetMapping("/commandes/data/entree-stock/etiquettes/{id}/{orderDate}")
    public ResponseEntity<Resource> getPdf(
        @PathVariable Integer id,
        @PathVariable LocalDate orderDate,
        @RequestParam(required = false, name = "startAt", defaultValue = "1") Integer startAt,
        HttpServletRequest request
    ) throws IOException {
        final Resource resource = stockEntryDataServicetryService.printEtiquette(new CommandeId(id, orderDate), startAt);
        return Utils.printPDF(resource, request);
    }

    @GetMapping("/commandes/data/entree-stock/pdf/{id}/{orderDate}")
    public ResponseEntity<Resource> getPdf(@PathVariable Integer id, @PathVariable LocalDate orderDate, HttpServletRequest request)
        throws IOException {
        final Resource resource = stockEntryDataServicetryService.exportToPdf(new CommandeId(id, orderDate));
        return Utils.printPDF(resource, request);
    }

    @GetMapping("/commandes/data/entree-stock/list-bon-livraison")
    public ResponseEntity<List<DeliveryReceiptProjection>> fetchListBons(@RequestParam("search") String search) {
        return ResponseEntity.ok().body(stockEntryDataServicetryService.fetchAllReceipts(search).getContent());
    }

    @GetMapping("/commandes/data/entree-stock/filter-items/{id}/{orderDate}")
    public ResponseEntity<List<DeliveryReceiptItemProjection>> fetchDetailBons(
        @PathVariable Integer id,
        @PathVariable LocalDate orderDate
    ) {
        return ResponseEntity.ok()
            .body(stockEntryDataServicetryService.findAllByCommandeIdAndCommandeOrderDate(new CommandeId(id, orderDate)));
    }
}
