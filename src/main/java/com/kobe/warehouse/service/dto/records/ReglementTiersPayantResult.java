package com.kobe.warehouse.service.dto.records;

import com.kobe.warehouse.service.dto.projection.ReglementTiersPayants;

import java.util.List;

public record ReglementTiersPayantResult(Integer totalElements, List<ReglementTiersPayants> content) {
}
