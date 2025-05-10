package com.kobe.warehouse.service.dci.service;

import com.kobe.warehouse.service.dci.dto.DciDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DciService {
    Page<DciDTO> findAll(String search, Pageable pageable);
}
