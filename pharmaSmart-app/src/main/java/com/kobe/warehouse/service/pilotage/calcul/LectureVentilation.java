package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import java.util.List;

/** Ce qui ne change pas entre la lecture de la période et celle de la référence. */
public record LectureVentilation(
    SourceAnalyse source,
    List<AxeAnalyse> axes,
    List<FiltreAnalyseDTO> filtres,
    Granularite granularite,
    TranchesRemise tranchesRemise
) {}
