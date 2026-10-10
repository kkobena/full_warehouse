package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Sous-section Différés & crédit : reste dû des ventes différées (à date), avoirs clients émis et remboursés (sur la période). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DifferesTresorerieDTO(
    long encours,
    List<TrancheMontantDTO> vieillissement,
    List<ComptageDTO> clients,
    CelluleAnalyseDTO avoirsEmis,
    CelluleAnalyseDTO avoirsRembourses
) {}
