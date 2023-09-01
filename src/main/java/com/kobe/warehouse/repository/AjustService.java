package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.filter.AjustementFilterRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AjustService {
    Page<AjustDTO> loadAll(AjustementFilterRecord ajustementFilterRecord, Pageable pageable);
}
