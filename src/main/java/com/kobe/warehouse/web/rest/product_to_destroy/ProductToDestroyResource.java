package com.kobe.warehouse.web.rest.product_to_destroy;

import com.kobe.warehouse.service.dto.records.Keys;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyPayload;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductsToDestroyPayload;
import com.kobe.warehouse.service.product_to_destroy.service.ProductsToDestroyService;
import com.kobe.warehouse.service.report.excel.ProductToDestroyExcelCsvReportService;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/product-to-destroy")
public class ProductToDestroyResource {

    private final ProductsToDestroyService productsToDestroyService;
    private final ProductToDestroyExcelCsvReportService productToDestroyExcelCsvReportService;

    public ProductToDestroyResource(
        ProductsToDestroyService productsToDestroyService,
        ProductToDestroyExcelCsvReportService productToDestroyExcelCsvReportService
    ) {
        this.productsToDestroyService = productsToDestroyService;
        this.productToDestroyExcelCsvReportService = productToDestroyExcelCsvReportService;
    }

    @PostMapping
    public ResponseEntity<Void> addLotQuantities(@Valid @RequestBody ProductsToDestroyPayload payload) {
        productsToDestroyService.addLotQuantities(payload);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductToDestroyDTO>> fetchAll(ProductToDestroyFilter productToDestroyFilter, Pageable pageable) {
        Page<ProductToDestroyDTO> page = productsToDestroyService.findAll(productToDestroyFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportToExcel(ProductToDestroyFilter productToDestroyFilter) {
        try {
            byte[] excelData = productToDestroyExcelCsvReportService.exportToExcel(productToDestroyFilter);
            String filename = buildFilename("produits_a_detruire", ".xlsx");
            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportToCsv(ProductToDestroyFilter productToDestroyFilter) {
        try {
            byte[] csvData = productToDestroyExcelCsvReportService.exportToCsv(productToDestroyFilter);
            String filename = buildFilename("produits_a_detruire", ".csv");
            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String buildFilename(String baseName, String extension) {
        return baseName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) + extension;
    }

    @GetMapping("/editing")
    public ResponseEntity<List<ProductToDestroyDTO>> fetchForEdit(ProductToDestroyFilter productToDestroyFilter, Pageable pageable) {
        Page<ProductToDestroyDTO> page = productsToDestroyService.findEditing(productToDestroyFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/sum")
    public ResponseEntity<ProductToDestroySumDTO> fetchSum(ProductToDestroyFilter productToDestroyFilter) {
        return ResponseEntity.ok().body(productsToDestroyService.getSum(productToDestroyFilter));
    }

    @PostMapping("/destroy")
    public ResponseEntity<Void> destroy(@RequestBody Keys keys) {
        productsToDestroyService.destroy(keys);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/add-product")
    public ResponseEntity<Void> addProductQuantity(@Valid @RequestBody ProductToDestroyPayload payload) {
        productsToDestroyService.addProductQuantity(payload);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/close")
    public ResponseEntity<Void> closeLastEdition() {
        productsToDestroyService.closeLastEdition();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/modify-product")
    public ResponseEntity<Void> modifyProductQuantity(@Valid @RequestBody ProductToDestroyPayload payload) {
        productsToDestroyService.modifyProductQuantity(payload);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deleteAll(@RequestBody Keys keys) {
        productsToDestroyService.remove(keys);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdf(ProductToDestroyFilter productToDestroyFilter) {
        return productsToDestroyService.generatePdf(productToDestroyFilter);
    }
}
