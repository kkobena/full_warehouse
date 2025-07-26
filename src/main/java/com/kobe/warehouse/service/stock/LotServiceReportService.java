package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface LotServiceReportService {
    ResponseEntity<byte[]> generatePdf(List<LotPerimeDTO> lotPerimes, LotPerimeValeurSum sum, LocalDate from, LocalDate to);
}
