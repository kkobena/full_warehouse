package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Une année comparée à la précédente : 12 mois (vides après le mois en cours), 4 trimestres et le total.
 *
 * @param complete année close ; l'année en cours ne l'est pas et son total se compare à la même date de l'année précédente
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AnneeCompareeDTO(int annee, boolean complete, List<CelluleAnalyseDTO> mois, List<CelluleAnalyseDTO> trimestres, CelluleAnalyseDTO total) {}
