package com.kobe.warehouse.web.rest.stat.produit;

import com.kobe.warehouse.service.report.RecapProduitVenduService;
import com.kobe.warehouse.service.stock.dto.RecapProduitVendu;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduRequestParam;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduSummary;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/recap-produit-vendu")
public class RecapProduitVenduResource {
    private final RecapProduitVenduService recapProduitVenduService;

    public RecapProduitVenduResource(RecapProduitVenduService recapProduitVenduService) {
        this.recapProduitVenduService = recapProduitVenduService;
    }

    @GetMapping
    public ResponseEntity<List<RecapProduitVendu>> getRecapProduitVenduReport(@Valid RecapProduitVenduRequestParam requestParam, Pageable pageable) {
        Page<RecapProduitVendu> page = recapProduitVenduService.getRecapProduitVenduReport(requestParam, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/invendus")
    public ResponseEntity<List<RecapProduitVendu>> getRecapProduitInvenduReport(@Valid RecapProduitVenduRequestParam requestParam, Pageable pageable) {
        Page<RecapProduitVendu> page = recapProduitVenduService.getRecapProduitInvenduReport(requestParam, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/summary")
    public ResponseEntity<RecapProduitVenduSummary> getRecapProduitVenduSummary(@Valid RecapProduitVenduRequestParam requestParam) {
        return ResponseEntity.ok().body(recapProduitVenduService.getRecapProduitVenduSummary(requestParam));
    }

    @GetMapping("/invendus/summary")
    public ResponseEntity<RecapProduitVenduSummary> getRecapProduitInvenduSummary(@Valid RecapProduitVenduRequestParam requestParam) {
        return ResponseEntity.ok().body(recapProduitVenduService.getRecapProduitInvenduSummary(requestParam));
    }

    /**
     * @param requestParam request parameters
     * @return Excel file (.xlsx)
     */
    @GetMapping(value = "/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportToExcel(@Valid RecapProduitVenduRequestParam requestParam
    ) {
        try {
            byte[] excelData = recapProduitVenduService.exportToExcel(requestParam);
            String filename = "recap_produit_vendu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss")) + ".xlsx";

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param requestParam request parameters
     * @return CSV file
     */
    @GetMapping(value = "/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@Valid RecapProduitVenduRequestParam requestParam
    ) {
        try {
            byte[] csvData = recapProduitVenduService.exportToCsv(requestParam);
            String filename = "recap_produit_vendu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss")) + ".csv";

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param requestParam request parameters
     * @return Excel file (.xlsx) for unsold products
     */
    @GetMapping(value = "/invendus/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportInvenduToExcel(@Valid RecapProduitVenduRequestParam requestParam
    ) {
        try {
            byte[] excelData = recapProduitVenduService.exportInvenduToExcel(requestParam);
            String filename = "recap_produit_invendu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss")) + ".xlsx";

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param requestParam request parameters
     * @return CSV file for unsold products
     */
    @GetMapping(value = "/invendus/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportInvenduToCsv(@Valid RecapProduitVenduRequestParam requestParam
    ) {
        try {
            byte[] csvData = recapProduitVenduService.exportInvenduToCsv(requestParam);
            String filename = "recap_produit_invendu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss")) + ".csv";

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
