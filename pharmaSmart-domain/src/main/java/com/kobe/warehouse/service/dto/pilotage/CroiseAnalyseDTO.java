package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Tableau croisé des deux axes, sur le premier indicateur. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CroiseAnalyseDTO(List<MembreAnalyseDTO> colonnes, List<LigneCroiseeDTO> lignes) {}
