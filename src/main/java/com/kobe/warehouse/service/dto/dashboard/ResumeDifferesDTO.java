package com.kobe.warehouse.service.dto.dashboard;

import java.util.List;

/**
 * DTO résumé des différés à relancer (échéances aujourd'hui et retards).
 */
public record ResumeDifferesDTO(
    Integer nombreEcheancesAujourdhui,
    Long montantTotalDu,
    List<DiffereARelancerDTO> differes
) {}

