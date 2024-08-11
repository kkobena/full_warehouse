package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.filter.AjustementFilterRecord;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AjustService {
    Page<AjustDTO> loadAll(AjustementFilterRecord ajustementFilterRecord, Pageable pageable);

    Resource exportToPdf(Long id) throws IOException;

    Optional<AjustDTO> getOneById(Long id);
}
