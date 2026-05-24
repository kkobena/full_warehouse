package com.kobe.warehouse.service.dto.records;

public record ImportLineErrorRecord(
    int lineNumber,
    String rawCode,
    String rawQuantity,
    String reason
) {

}
