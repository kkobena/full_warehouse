package com.kobe.warehouse.service.dto.report;

import java.util.List;

public record EncoursMensuelDTO(
    List<String> labels,
    List<Long> montantFacture,
    List<Long> encoursRestant
) {}
