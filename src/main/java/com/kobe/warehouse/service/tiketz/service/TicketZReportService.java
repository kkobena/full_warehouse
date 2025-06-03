package com.kobe.warehouse.service.tiketz.service;

import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import org.springframework.http.ResponseEntity;

public interface TicketZReportService {
    ResponseEntity<byte[]> generatePdf(TicketZ ticket, Pair periode);
}
