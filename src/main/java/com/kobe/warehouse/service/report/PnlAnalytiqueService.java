package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.PnlEvolutionDTO;
import com.kobe.warehouse.service.dto.report.PnlFamilleDTO;
import com.kobe.warehouse.service.dto.report.PnlSegmentDTO;
import java.util.List;

public interface PnlAnalytiqueService {

    List<PnlSegmentDTO> getSnapshotBySegment(int year);

    List<PnlFamilleDTO> getSnapshotByFamille(int year);

    PnlEvolutionDTO getEvolutionByFamille();

    PnlEvolutionDTO getEvolutionBySegment();
}
