package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Sous-section Équipe : un vendeur par ligne, et l'équipe entière pour situer chacun. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EquipePilotageDTO(ComparaisonPeriodesDTO comparaison, List<LigneVendeurDTO> vendeurs, LigneVendeurDTO equipe) {}
