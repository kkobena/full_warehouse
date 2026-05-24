package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.service.facturation.dto.AvoirCommand;
import com.kobe.warehouse.service.facturation.dto.AvoirDto;
import com.kobe.warehouse.service.facturation.dto.AvoirSearchParams;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AvoirService {
    AvoirDto creerAvoir(AvoirCommand command);

    AvoirDto emettre(Long avoirId);

    void imputer(Long avoirId, Long factureId, LocalDate factureDate);

    void annuler(Long avoirId, String motif);

    Page<AvoirDto> findAll(AvoirSearchParams params, Pageable pageable);

    byte[] exportPdf(Long avoirId);

    byte[] exportExcel(AvoirSearchParams params);

    byte[] exportListPdf(AvoirSearchParams params);
}
