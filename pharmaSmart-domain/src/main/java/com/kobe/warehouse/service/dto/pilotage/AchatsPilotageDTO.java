package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Sous-section Achats de « Achats & stock » : achats datés à la réception (décision du 2026-10-09). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AchatsPilotageDTO(
    ComparaisonPeriodesDTO comparaison,
    CelluleAnalyseDTO achatsTtc,
    CelluleAnalyseDTO achatsHt,
    CelluleAnalyseDTO nbBons,
    CelluleAnalyseDTO delaiMoyen,
    CelluleAnalyseDTO conformite,
    List<LigneAchatsDTO> fournisseurs,
    List<LigneAchatsDTO> familles
) {}
