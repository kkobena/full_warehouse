package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.BfrEvolutionDTO;
import com.kobe.warehouse.service.dto.report.BfrSnapshotDTO;

public interface CashFlowBfrService {

    BfrSnapshotDTO getSnapshot();

    BfrEvolutionDTO getEvolution();
}
