package com.kobe.warehouse.service.dto.report;

import java.util.List;

public record BfrEvolutionDTO(
    List<String> labels,
    List<Long> creancesEmises,
    List<Long> achatsRecus
) {}
