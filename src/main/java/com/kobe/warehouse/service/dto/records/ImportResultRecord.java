package com.kobe.warehouse.service.dto.records;

import java.util.List;

public record ImportResultRecord(
    int imported,
    int ignored,
    int rejected,
    List<ImportLineErrorRecord> errors
) {

}
