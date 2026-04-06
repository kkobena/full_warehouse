package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.service.facturation.dto.EtatRapprochementDto;
import com.kobe.warehouse.service.facturation.dto.RapprochementParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RapprochementService {
    Page<EtatRapprochementDto> getEtatRapprochement(RapprochementParams params, Pageable pageable);

    byte[] exportPdf(RapprochementParams params);

    byte[] exportExcel(RapprochementParams params);
}
