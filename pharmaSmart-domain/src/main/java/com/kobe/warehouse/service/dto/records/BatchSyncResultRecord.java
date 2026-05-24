package com.kobe.warehouse.service.dto.records;

import java.util.List;

public record BatchSyncResultRecord(
    int saved,
    int failed,
    List<Long> failedIds
) {}
