package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import com.kobe.warehouse.service.report.StockRotationReportService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StockRotationReportResource {

    private final StockRotationReportService stockRotationReportService;

    public StockRotationReportResource(StockRotationReportService stockRotationReportService) {
        this.stockRotationReportService = stockRotationReportService;
    }

    /**
     * GET /stock/rotation : Get all stock rotation data
     *
     * @return List of stock rotation records
     */
    @GetMapping("/stock/rotation")
    public ResponseEntity<List<StockRotationDTO>> getAllStockRotation() {
        List<StockRotationDTO> rotations = stockRotationReportService.getAllStockRotation();
        return ResponseEntity.ok().body(rotations);
    }

    /**
     * GET /stock/rotation/category : Get stock rotation by category
     *
     * @param categorie The category name to filter by
     * @return List of stock rotation records for the category
     */
    @GetMapping("/stock/rotation/category")
    public ResponseEntity<List<StockRotationDTO>> getStockRotationByCategory(@RequestParam String categorie) {
        List<StockRotationDTO> rotations = stockRotationReportService.getStockRotationByCategory(categorie);
        return ResponseEntity.ok().body(rotations);
    }

    /**
     * GET /stock/rotation/abc : Get stock rotation by ABC classification
     *
     * @param categorieABC The ABC classification to filter by (A, B, C)
     * @return List of stock rotation records for the ABC classification
     */
    @GetMapping("/stock/rotation/abc")
    public ResponseEntity<List<StockRotationDTO>> getStockRotationByABCClassification(
        @RequestParam CategorieABC categorieABC
    ) {
        List<StockRotationDTO> rotations = stockRotationReportService.getStockRotationByABCClassification(categorieABC);
        return ResponseEntity.ok().body(rotations);
    }

    /**
     * GET /stock/rotation/count : Get count of products by ABC classification
     *
     * @return Map of ABC classification to count
     */
    @GetMapping("/stock/rotation/count")
    public ResponseEntity<Map<CategorieABC, Long>> getStockRotationCountByABCClassification() {
        Map<CategorieABC, Long> counts = stockRotationReportService.getStockRotationCountByABCClassification();
        return ResponseEntity.ok().body(counts);
    }

    /**
     * GET /stock/rotation/slow : Get slow moving products
     *
     * @return List of slow moving products (Category C)
     */
    @GetMapping("/stock/rotation/slow")
    public ResponseEntity<List<StockRotationDTO>> getSlowMovingProducts() {
        List<StockRotationDTO> slowProducts = stockRotationReportService.getSlowMovingProducts();
        return ResponseEntity.ok().body(slowProducts);
    }

    /**
     * GET /stock/rotation/export : Export stock rotation report as PDF
     *
     * @return PDF file
     */
    @GetMapping(value = "/stock/rotation/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportStockRotationToPdf() {
        byte[] pdf = stockRotationReportService.exportStockRotationToPdf();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=stock-rotation.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
