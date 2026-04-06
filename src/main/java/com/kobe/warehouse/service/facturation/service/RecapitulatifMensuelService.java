package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelDto;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecapitulatifMensuelService {
    Page<RecapitulatifMensuelDto> getRecapitulatif(RecapitulatifMensuelParams params, Pageable pageable);

    byte[] exportPdf(RecapitulatifMensuelParams params);

    byte[] exportExcel(RecapitulatifMensuelParams params);
}
