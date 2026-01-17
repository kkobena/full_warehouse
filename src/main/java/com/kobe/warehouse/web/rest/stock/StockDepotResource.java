package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.report.excel.StockDepotExcelCsvReportService;
import com.kobe.warehouse.service.stock.GestionStockDepotService;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.io.IOException;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/stock-depots")
public class StockDepotResource {

    private final GestionStockDepotService gestionStockDepotService;
    private final StockDepotExcelCsvReportService stockDepotExcelCsvReportService;

    public StockDepotResource(
        GestionStockDepotService gestionStockDepotService,
        StockDepotExcelCsvReportService stockDepotExcelCsvReportService
    ) {
        this.gestionStockDepotService = gestionStockDepotService;
        this.stockDepotExcelCsvReportService = stockDepotExcelCsvReportService;
    }

    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getStock(
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(name = "magasinId") Integer magasinId,
        @RequestParam(required = false, name = "storageId") Integer storageId,
        @RequestParam(required = false, name = "rayonId") Integer rayonId,
        @RequestParam(required = false, name = "deconditionne") Boolean deconditionne,
        @RequestParam(required = false, name = "deconditionnable") Boolean deconditionnable,
        @RequestParam(required = false, name = "status") Status status,
        @RequestParam(required = false, name = "tableauNot") Integer tableauNot,
        Pageable pageable
    ) {
        Page<ProduitDTO> page = gestionStockDepotService.findAll(
            new ProduitCriteria()
                .setSearch(search)
                .setStatus(status)
                .setDeconditionnable(deconditionnable)
                .setDeconditionne(deconditionne)
                .setMagasinId(magasinId)
                .setTableauNot(tableauNot)
                .setRayonId(rayonId)
                .setStorageId(storageId),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/sales")
    public ResponseEntity<List<DepotExtensionSaleDTO>> getAllSales(
        @RequestParam(name = "magasinId", required = false) Long magasinId,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate,
        @RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "paymentStatus", required = false) PaymentStatus paymentStatus,
        Pageable pageable
    ) {
        Page<DepotExtensionSaleDTO> page = gestionStockDepotService.getVenteDepot(
            paymentStatus,
            magasinId,
            search,
            fromDate,
            toDate,
            userId,
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping(value = "/export/{id}/{saleDate}/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportToExcel(
        @PathVariable("id") Long id,
        @PathVariable("saleDate") LocalDate saleDate
    ) {
        try {
            byte[] excelData = stockDepotExcelCsvReportService.exportToExcel(new SaleId(id, saleDate));
            String filename = buildFilename("vente_depot_stock_" + id + "_" + saleDate, ".xlsx");
            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/export/{id}/{saleDate}/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportToCsv(
        @PathVariable("id") Long id,
        @PathVariable("saleDate") LocalDate saleDate
    ) {
        try {
            byte[] csvData = stockDepotExcelCsvReportService.exportToCsv(new SaleId(id, saleDate));
            String filename = buildFilename("vente_depot_stock_" + id + "_" + saleDate, ".csv");
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
}
