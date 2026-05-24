package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.TiersPayantCreancesSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO;
import com.kobe.warehouse.service.report.TiersPayantReportService;
import java.time.LocalDate;
import java.util.List;

import com.kobe.warehouse.service.report.pdf.TiersPayantPdfReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TiersPayantReportResource {

    private final TiersPayantReportService tiersPayantReportService;
    private final   TiersPayantPdfReportService tiersPayantPdfReportService;

    public TiersPayantReportResource(TiersPayantReportService tiersPayantReportService, TiersPayantPdfReportService tiersPayantPdfReportService) {
        this.tiersPayantReportService = tiersPayantReportService;
        this.tiersPayantPdfReportService = tiersPayantPdfReportService;
    }

    /**
     * GET /tiers-payant/creances : Get unpaid invoices (créances)
     *
     * @param groupeTiersPayantId Optional groupe tiers payant ID filter
     * @param ageCategory Optional age category filter
     * @return List of unpaid invoices
     */
    @GetMapping("/tiers-payant/creances")
    public ResponseEntity<List<TiersPayantInvoiceDTO>> getUnpaidInvoices(
        @RequestParam(required = false) Integer groupeTiersPayantId,
        @RequestParam(required = false) TiersPayantInvoiceDTO.AgeCategory ageCategory
    ) {
        List<TiersPayantInvoiceDTO> invoices = tiersPayantReportService.getUnpaidInvoices(groupeTiersPayantId, ageCategory);
        return ResponseEntity.ok().body(invoices);
    }

    /**
     * GET /tiers-payant/creances/summary : Get créances summary by groupe tiers payant
     *
     * @return List of créances summaries
     */
    @GetMapping("/tiers-payant/creances/summary")
    public ResponseEntity<List<TiersPayantCreancesSummaryDTO>> getCreancesSummary() {
        List<TiersPayantCreancesSummaryDTO> summary = tiersPayantReportService.getCreancesSummary();
        return ResponseEntity.ok().body(summary);
    }

    /**
     * GET /tiers-payant/payment-history : Get payment history
     *
     * @param groupeTiersPayantId Optional groupe tiers payant ID filter
     * @param startDate Start date (defaults to 30 days ago)
     * @param endDate End date (defaults to today)
     * @return List of paid invoices
     */
    @GetMapping("/tiers-payant/payment-history")
    public ResponseEntity<List<TiersPayantInvoiceDTO>> getPaymentHistory(
        @RequestParam(required = false) Integer groupeTiersPayantId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        List<TiersPayantInvoiceDTO> history = tiersPayantReportService.getPaymentHistory(groupeTiersPayantId, start, end);
        return ResponseEntity.ok().body(history);
    }

    /**
     * GET /tiers-payant/creances/export : Export créances report as PDF
     *
     * @return PDF file
     */
    @GetMapping(value = "/tiers-payant/creances/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportCreancesToPdf() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=creances-tiers-payant.pdf");
        return ResponseEntity.ok().headers(headers).body(tiersPayantPdfReportService.export());
    }
}
