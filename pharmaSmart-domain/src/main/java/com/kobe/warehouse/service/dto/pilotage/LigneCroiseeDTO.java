package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Une ligne du croisé : une cellule par colonne, sur le premier indicateur. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneCroiseeDTO(String cle, String libelle, List<CelluleAnalyseDTO> cellules) {}
