package com.kobe.warehouse.service.dto.report;

public record BfrSnapshotDTO(
    Long stockValue,
    Long creancesTp,
    Long dettesFournisseurs,
    Long bfr,
    Integer dio,
    Integer dso,
    Integer dpo,
    Integer ccc
) {}
