package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.ClientRetentionKpiDTO;
import com.kobe.warehouse.service.dto.report.ClientRetentionRowDTO;
import java.util.List;

public interface ClientRetentionReportService {

    ClientRetentionKpiDTO getKpi();

    List<ClientRetentionRowDTO> getClientList(int limit);
}
