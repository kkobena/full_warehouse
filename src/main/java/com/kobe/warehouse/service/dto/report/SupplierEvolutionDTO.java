package com.kobe.warehouse.service.dto.report;

import java.util.List;

public record SupplierEvolutionDTO(
    List<String> labels,
    List<Long> montantsN,
    List<Long> montantsN1,
    List<Integer> delaisN,
    List<Integer> delaisN1,
    List<Integer> nbCommandesN,
    List<Integer> nbCommandesN1
) {}
