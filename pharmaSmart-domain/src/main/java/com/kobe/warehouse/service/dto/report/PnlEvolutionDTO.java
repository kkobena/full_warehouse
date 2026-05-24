package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record PnlEvolutionDTO(
    List<String> labels,
    List<PnlEvolutionSerieDTO> series
) {
    public record PnlEvolutionSerieDTO(
        String famille,
        String segment,
        List<BigDecimal> tauxMargeValues
    ) {}
}
