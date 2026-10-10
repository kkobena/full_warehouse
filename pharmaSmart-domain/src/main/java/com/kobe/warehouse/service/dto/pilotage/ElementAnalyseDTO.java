package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Un élément de la ventilation : une cellule par indicateur demandé.
 *
 * @param part part du total sur le premier indicateur, s'il est additif (%)
 * @param contribution part de l'écart total sur le premier indicateur, s'il est additif (%)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ElementAnalyseDTO(String cle, String libelle, List<CelluleAnalyseDTO> cellules, Double part, Double contribution) {}
