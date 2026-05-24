package com.kobe.warehouse.service.dto.dashboard;

public record RotationStockDTO(
    Double rotationMoyenne,
    Integer rapide,  // > 4x
    Integer normal,  // 2-4x
    Integer lent     // < 2x
) {
}
