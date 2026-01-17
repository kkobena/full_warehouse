package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.report.excel.LotPerimeExcelCsvReportService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class LotResource {

    private final LotService lotService;
    private final LotPerimeExcelCsvReportService lotPerimeExcelCsvReportService;

    public LotResource(LotService lotService, LotPerimeExcelCsvReportService lotPerimeExcelCsvReportService) {
        this.lotService = lotService;
        this.lotPerimeExcelCsvReportService = lotPerimeExcelCsvReportService;
    }

    @PostMapping("/lot/add-to-commande")
    public ResponseEntity<LotDTO> addLotToCommande(@Valid @RequestBody LotDTO lot) {
        return ResponseEntity.ok(lotService.addLot(lot));
    }

    @PutMapping("/lot/remove-to-commande")
    public ResponseEntity<Void> removeLotToCommande(@RequestBody LotDTO lot) {
        lotService.remove(lot);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/lot/add")
    public ResponseEntity<LotDTO> add(@Valid @RequestBody LotDTO lot) {
        return ResponseEntity.ok().body(lotService.addLot(lot));
    }

    @PostMapping("/lot/edit")
    public ResponseEntity<LotDTO> edit(@Valid @RequestBody LotDTO lot) {
        return ResponseEntity.ok().body(lotService.editLot(lot));
    }

    @DeleteMapping("/lot/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        lotService.remove(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/lot")
    public ResponseEntity<List<LotPerimeDTO>> fetchAll(LotFilterParam lotFilterParam, Pageable pageable) {
        Page<LotPerimeDTO> page = lotService.findLotsPerimes(lotFilterParam, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/lot/sum")
    public ResponseEntity<LotPerimeValeurSum> fetchSum(LotFilterParam lotFilterParam) {
        return ResponseEntity.ok().body(lotService.findPerimeSum(lotFilterParam));
    }

    @GetMapping("/lot/pdf")
    public ResponseEntity<byte[]> generatePdf(LotFilterParam lotFilterParam) {
        return lotService.generatePdf(lotFilterParam);
    }

    @GetMapping(value = "/lot/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportToExcel(LotFilterParam lotFilterParam) {
        try {
            byte[] excelData = lotPerimeExcelCsvReportService.exportToExcel(lotFilterParam);
            String filename = buildFilename("produits_perimes", ".xlsx");
            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lot/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportToCsv(LotFilterParam lotFilterParam) {
        try {
            byte[] csvData = lotPerimeExcelCsvReportService.exportToCsv(lotFilterParam);
            String filename = buildFilename("produits_perimes", ".csv");
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
