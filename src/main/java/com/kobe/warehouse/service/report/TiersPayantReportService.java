package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.TiersPayantCreancesSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO;
import java.time.LocalDate;
import java.util.List;

public interface TiersPayantReportService {
    /**
     * Get unpaid invoices (créances) with optional filters
     *
     * @param groupeTiersPayantId Optional groupe tiers payant ID filter
     * @param ageCategory Optional age category filter
     * @return List of unpaid invoices
     */
    List<TiersPayantInvoiceDTO> getUnpaidInvoices(Integer groupeTiersPayantId, TiersPayantInvoiceDTO.AgeCategory ageCategory);

    /**
     * Get créances summary by groupe tiers payant
     *
     * @return List of créances summaries
     */
    List<TiersPayantCreancesSummaryDTO> getCreancesSummary();

    /**
     * Get payment history for a specific groupe tiers payant
     *
     * @param groupeTiersPayantId Groupe tiers payant ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of paid invoices
     */
    List<TiersPayantInvoiceDTO> getPaymentHistory(Integer groupeTiersPayantId, LocalDate startDate, LocalDate endDate);

    /**
     * Export créances report as PDF
     *
     * @return PDF bytes
     */
    byte[] exportCreancesToPdf();
}
