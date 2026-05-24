package com.kobe.warehouse.service.dto.report;

import java.util.List;

public record ConcentrationEvolutionDTO(
    List<String> labels,
    List<ConcentrationEvolutionSerieDTO> series
) {
    public record ConcentrationEvolutionSerieDTO(
        String organisme,
        List<Long> caValues
    ) {}
}
