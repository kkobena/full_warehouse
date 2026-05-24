package com.kobe.warehouse.service.dto.nav;

public record NavReorderDTO(
    Integer navItemId,
    Integer newOrdre,
    Integer newParentId
) {}

