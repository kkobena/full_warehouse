package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class SalesDataResource {

    private final Logger log = LoggerFactory.getLogger(SalesDataResource.class);
    private final SaleDataService saleDataService;


    public SalesDataResource(SaleDataService saleDataService) {
        this.saleDataService = saleDataService;

    }

    /**
     * {@code GET /sales/:id} : get the "id" sales.
     *
     * @param id the id of the sales to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the sales, or
     * with status {@code 404 (Not Found)}.
     */
    @GetMapping("/sales/{id}/{saleDate}")
    public ResponseEntity<SaleDTO> getSales(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        log.debug("REST request to get Sales : {}", id);
        SaleDTO sale = saleDataService.fetchPurchaseBy(id, saleDate);
        return ResponseEntity.ok().body(sale);
    }

    @GetMapping("/sales/edit/{id}/{saleDate}")
    public ResponseEntity<SaleDTO> getSalesForEdit(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        log.debug("REST request to get Sales : {}", id);
        Optional<SaleDTO> saleDTO = saleDataService.fetchPurchaseForEditBy(id, saleDate);
        return saleDTO.map(dto -> ResponseEntity.ok().body(dto)).orElseGet(() -> ResponseEntity.ok().build());
    }

    @GetMapping("/sales/print/invoice/{id}/{saleDate}")
    public ResponseEntity<Resource> printInvoice(
        @PathVariable("id") Long id,
        @PathVariable("saleDate") LocalDate saleDate,
        HttpServletRequest request
    ) throws IOException {
        Resource resource = saleDataService.printInvoice(new SaleId(id, saleDate));
        return Utils.printPDF(resource, request);
    }

    @GetMapping("/sales/prevente")
    public ResponseEntity<List<SaleDTO>> getAllSalesPreventes(
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "type", required = false) String typeVente,
        @RequestParam(name = "userId", required = false) Long userId
    ) {
        log.debug("REST request to get a page of Sales");
        List<SaleDTO> data = saleDataService.allPrevente(search, typeVente, userId);
        return ResponseEntity.ok().body(data);
    }

    @GetMapping("/sales")
    public ResponseEntity<List<SaleDTO>> getAllSales(
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate,
        @RequestParam(name = "fromHour", required = false) String fromHour,
        @RequestParam(name = "toHour", required = false) String toHour,
        @RequestParam(name = "global", required = false) Boolean global,
        @RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "type", required = false) String type,
        @RequestParam(name = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of Sales");
        Page<SaleDTO> page = saleDataService.listVenteTerminees(
            search,
            fromDate,
            toDate,
            fromHour,
            toHour,
            global,
            userId,
            type,
            null,
            null, categorieChiffreAffaires,
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }


    @GetMapping("/sales/print/receipt/{id}/{saleDate}")
    public ResponseEntity<Void> printCashReceipt(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleDataService.printReceipt(new SaleId(id, saleDate), false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sales/re-print/receipt/{id}/{saleDate}")
    public ResponseEntity<Void> rePrintCashReceipt(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleDataService.printReceipt(new SaleId(id, saleDate), true);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sales/assurance/print/receipt/{id}/{saleDate}")
    public ResponseEntity<Void> printVoReceipt(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleDataService.printReceipt(new SaleId(id, saleDate), false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sales/assurance/re-print/receipt/{id}/{saleDate}")
    public ResponseEntity<Void> rePrintVoReceipt(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleDataService.printReceipt(new SaleId(id, saleDate), true);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sales/receipt/tauri/{id}/{saleDate}")
    public ResponseEntity<byte[]> getReceiptForTauri(
        @PathVariable("id") Long id,
        @PathVariable("saleDate") LocalDate saleDate,
        @RequestParam(value = "isEdition", required = false) boolean isEdition
    ) {
        log.debug("REST request to get ESC/POS receipt for Tauri client: sale id {}, date {}", id, saleDate);
        try {
            byte[] escPosData = saleDataService.generateEscPosReceipt(new SaleId(id, saleDate), isEdition);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receipt.bin\"")
                .body(escPosData);
        } catch (IOException e) {
            log.error("Error generating ESC/POS receipt for Tauri client", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
