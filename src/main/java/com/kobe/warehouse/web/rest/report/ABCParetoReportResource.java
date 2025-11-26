package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.domain.enumeration.ClassePareto;
import com.kobe.warehouse.service.dto.report.ABCParetoDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import com.kobe.warehouse.service.report.ABCParetoReportService;
import java.util.List;

import com.kobe.warehouse.service.report.pdf.ABCParetoPdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ABCParetoReportResource {

    private final ABCParetoReportService abcParetoReportService;
    private final ABCParetoPdfReportService abcParetoPdfReportService;

    public ABCParetoReportResource(ABCParetoReportService abcParetoReportService, ABCParetoPdfReportService abcParetoPdfReportService) {
        this.abcParetoReportService = abcParetoReportService;
        this.abcParetoPdfReportService = abcParetoPdfReportService;
    }

    /**
     * GET /abc-pareto : Get all ABC Pareto analysis data
     *
     * @return List of ABC Pareto records
     */
    @GetMapping("/abc-pareto")
    public ResponseEntity<List<ABCParetoDTO>> getAllABCParetoAnalysis() {
        List<ABCParetoDTO> paretoAnalysis = abcParetoReportService.getAllABCParetoAnalysis();
        return ResponseEntity.ok().body(paretoAnalysis);
    }

    /**
     * GET /abc-pareto/category : Get ABC Pareto analysis by category
     *
     * @param categorie The category name to filter by
     * @return List of ABC Pareto records for the category
     */
    @GetMapping("/abc-pareto/category")
    public ResponseEntity<List<ABCParetoDTO>> getABCParetoByCategory(@RequestParam String categorie) {
        List<ABCParetoDTO> paretoAnalysis = abcParetoReportService.getABCParetoByCategory(categorie);
        return ResponseEntity.ok().body(paretoAnalysis);
    }

    /**
     * GET /abc-pareto/class : Get ABC Pareto analysis by Pareto class
     *
     * @param classePareto The Pareto classification to filter by (A, B, C)
     * @return List of ABC Pareto records for the class
     */
    @GetMapping("/abc-pareto/class")
    public ResponseEntity<List<ABCParetoDTO>> getABCParetoByClass(@RequestParam ClassePareto classePareto) {
        List<ABCParetoDTO> paretoAnalysis = abcParetoReportService.getABCParetoByClass(classePareto);
        return ResponseEntity.ok().body(paretoAnalysis);
    }

    /**
     * GET /abc-pareto/top : Get top N products by revenue contribution
     *
     * @param limit Number of products to return (default: 20)
     * @return List of top revenue contributors
     */
    @GetMapping("/abc-pareto/top")
    public ResponseEntity<List<ABCParetoDTO>> getTopRevenueContributors(@RequestParam(defaultValue = "20") int limit) {
        List<ABCParetoDTO> topContributors = abcParetoReportService.getTopRevenueContributors(limit);
        return ResponseEntity.ok().body(topContributors);
    }

    /**
     * GET /abc-pareto/summary : Get aggregated ABC Pareto summary
     *
     * @return ABC Pareto summary with class distribution
     */
    @GetMapping("/abc-pareto/summary")
    public ResponseEntity<ABCParetoSummaryDTO> getABCParetoSummary() {
        ABCParetoSummaryDTO summary = abcParetoReportService.getABCParetoSummary();
        return ResponseEntity.ok().body(summary);
    }

    /**
     * GET /abc-pareto/export : Export ABC Pareto report as PDF
     *
     * @return PDF file
     */
    @GetMapping(value = "/abc-pareto/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportABCParetoToPdf() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=abc_pareto_report.pdf");
        return ResponseEntity.ok().headers(headers).body(abcParetoPdfReportService.export());
    }
}
