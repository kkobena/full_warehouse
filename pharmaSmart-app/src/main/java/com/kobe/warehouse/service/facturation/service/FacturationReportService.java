package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import java.util.List;
import org.springframework.core.io.Resource;

public interface FacturationReportService {
    Resource printToPdf(List<FactureTiersPayant> factureTiersPayants) throws ReportFileExportException;

    Resource printToPdf(FactureTiersPayant factureTiersPayant) throws ReportFileExportException;

    List<String> print(List<FactureTiersPayant> factureTiersPayants);
}
