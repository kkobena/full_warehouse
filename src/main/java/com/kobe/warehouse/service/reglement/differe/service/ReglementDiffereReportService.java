package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.differe.dto.*;
import org.springframework.core.io.Resource;

import java.util.List;

public interface ReglementDiffereReportService {

    Resource printListToPdf(List<DiffereDTO> differe, DiffereSummary differeSummary) throws ReportFileExportException;

    Resource printReglementToPdf(List<ReglementDiffereWrapperDTO> reglements, DifferePaymentSummaryDTO differePaymentSummary, ReportPeriode reportPeriode) throws ReportFileExportException;
}
