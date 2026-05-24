package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.DemarqueByMotifDTO;
import com.kobe.warehouse.service.dto.report.DemarqueKpiDTO;
import java.time.LocalDate;
import java.util.List;

public interface DemarqueReportService {

    DemarqueKpiDTO getKpi(LocalDate startDate, LocalDate endDate);

    List<DemarqueByMotifDTO> getByMotif(LocalDate startDate, LocalDate endDate);
}
