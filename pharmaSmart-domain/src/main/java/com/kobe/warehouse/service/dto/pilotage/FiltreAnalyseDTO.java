package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import java.util.List;

/** Restreint une analyse aux éléments d'un axe (descente) ; une clé vide désigne l'élément « sans » (sans famille…). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FiltreAnalyseDTO(AxeAnalyse axe, List<String> cles) {}
