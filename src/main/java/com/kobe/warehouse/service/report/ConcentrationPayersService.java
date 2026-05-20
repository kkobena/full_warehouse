package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.ConcentrationEvolutionDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationOrganismeDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationSummaryDTO;
import java.util.List;

public interface ConcentrationPayersService {

    ConcentrationSummaryDTO getSummary(String periode, int topN);

    List<ConcentrationOrganismeDTO> getOrganismes(String periode, int topN);

    ConcentrationEvolutionDTO getEvolution(int topN);
}
