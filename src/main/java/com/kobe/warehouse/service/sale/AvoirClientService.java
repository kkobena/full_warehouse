package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.service.sale.dto.AvoirClientDTO;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AvoirClientService {
    Page<AvoirClientDTO> findAvoirs(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}
