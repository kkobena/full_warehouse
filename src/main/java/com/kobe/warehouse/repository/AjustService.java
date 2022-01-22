package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.AjustDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AjustService {
    Page<AjustDTO> loadAll(String search, LocalDate dtStart,LocalDate dtEnd, Pageable pageable);
}
