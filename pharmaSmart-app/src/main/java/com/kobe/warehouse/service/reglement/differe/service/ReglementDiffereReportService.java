package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.differe.dto.*;
import java.util.List;

public interface ReglementDiffereReportService {
    byte[] printListToPdf(List<DiffereDTO> differe, DiffereSummary differeSummary) throws ReportFileExportException;

    byte[] printReglementToPdf(
        List<ReglementDiffereWrapperDTO> reglements,
        DifferePaymentSummaryDTO differePaymentSummary,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException;
}
