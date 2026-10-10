package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Ce qu'un axe explique de l'écart : la part concentrée sur ses principaux éléments allant dans le sens de l'écart.
 *
 * @param part part de l'écart portée par {@code principales} (%, plafonnée à 100)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AxeExpliqueDTO(AxeAnalyseDTO axe, double part, int nombreElements, List<ContributionDTO> principales) {}
