package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Sous-section Achats / ventes : ce qui entre face à ce qui sort, au coût d'achat HT. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AchatsVentesPilotageDTO(ComparaisonPeriodesDTO comparaison, List<PointAchatsVentesDTO> points, List<LigneAchatsVentesDTO> familles) {}
