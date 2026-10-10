package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** « Expliquer l'écart » : les axes classés du plus explicatif au moins explicatif. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ExplicationEcartDTO(IndicateurPilotageDTO indicateur, Double valeur, Double valeurReference, Double ecart, List<AxeExpliqueDTO> axes) {}
