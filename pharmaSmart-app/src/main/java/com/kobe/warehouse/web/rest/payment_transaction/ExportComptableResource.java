package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.service.ap.ExportComptableService;
import com.kobe.warehouse.web.rest.Utils;
import java.io.IOException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finances")
public class ExportComptableResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExportComptableResource.class);

    private final ExportComptableService exportComptableService;

    public ExportComptableResource(ExportComptableService exportComptableService) {
        this.exportComptableService = exportComptableService;
    }

    @GetMapping("/export-comptable")
    public ResponseEntity<byte[]> exportComptable(
        @RequestParam(value = "startDate") LocalDate startDate,
        @RequestParam(value = "endDate") LocalDate endDate,
        @RequestParam(value = "format", defaultValue = "excel") String format,
        @RequestParam(value = "ventes", defaultValue = "true") boolean ventes,
        @RequestParam(value = "achats", defaultValue = "true") boolean achats,
        @RequestParam(value = "mvtCaisse", defaultValue = "true") boolean mvtCaisse,
        @RequestParam(value = "tiersPayant", defaultValue = "true") boolean tiersPayant,
        @RequestParam(value = "differes", defaultValue = "true") boolean differes,
        @RequestParam(value = "tva", defaultValue = "true") boolean tva
    ) {
        try {
            byte[] data = exportComptableService.export(startDate, endDate, format, ventes, achats, mvtCaisse, tiersPayant, differes, tva);
            return switch (format) {
                case "csv" -> ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .header("Content-Disposition", "attachment; filename=\"export-comptable.csv\"")
                    .body(data);
                case "pdf" -> Utils.printPDF(data, "export-comptable.pdf");
                default -> Utils.exportExcel(data, "export-comptable.xlsx");
            };
        } catch (IOException e) {
            LOG.error("Erreur lors de l'export comptable", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
