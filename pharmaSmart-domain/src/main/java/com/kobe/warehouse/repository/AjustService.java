package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.filter.AjustementFilterRecord;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AjustService {
    Page<AjustDTO> loadAll(AjustementFilterRecord ajustementFilterRecord, Pageable pageable);

    byte[] exportToPdf(Integer id);

    Optional<AjustDTO> getOneById(Integer id);
}
