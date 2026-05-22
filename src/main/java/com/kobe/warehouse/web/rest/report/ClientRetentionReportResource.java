package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.ClientRetentionKpiDTO;
import com.kobe.warehouse.service.dto.report.ClientRetentionRowDTO;
import com.kobe.warehouse.service.report.ClientRetentionReportService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-retention")
public class ClientRetentionReportResource {

    private final ClientRetentionReportService clientRetentionReportService;

    public ClientRetentionReportResource(ClientRetentionReportService clientRetentionReportService) {
        this.clientRetentionReportService = clientRetentionReportService;
    }

    @GetMapping("/kpi")
    public ResponseEntity<ClientRetentionKpiDTO> getKpi() {
        return ResponseEntity.ok(clientRetentionReportService.getKpi());
    }

    @GetMapping("/list")
    public ResponseEntity<List<ClientRetentionRowDTO>> getClientList(
        @RequestParam(defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(clientRetentionReportService.getClientList(limit));
    }
}
