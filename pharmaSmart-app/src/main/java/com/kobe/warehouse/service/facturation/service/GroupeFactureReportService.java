package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.facturation.dto.GroupeFactureDto;
import java.util.List;
import org.springframework.core.io.Resource;

public interface GroupeFactureReportService {
    Resource printToPdf(List<GroupeFactureDto> factureTiersPayants) throws ReportFileExportException;

    Resource printToPdf(GroupeFactureDto factureTiersPayant) throws ReportFileExportException;
}
