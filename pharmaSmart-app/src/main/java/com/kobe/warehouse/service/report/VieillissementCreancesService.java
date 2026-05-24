package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.DsoOrganismeDTO;
import com.kobe.warehouse.service.dto.report.EncoursMensuelDTO;
import com.kobe.warehouse.service.dto.report.VieillissementGlobalDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VieillissementCreancesService {

    VieillissementGlobalDTO getAgingGlobal();

    Page<DsoOrganismeDTO> getDsoByOrganisme(Pageable pageable);

    EncoursMensuelDTO getEncoursMensuelEvolution();
}
