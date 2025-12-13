package com.kobe.warehouse.service.reassort.dto;

import com.kobe.warehouse.service.errors.GenericError;

public record RepartionQueryDto(Integer sourceId, Integer destinationId, int quantity) {
    public RepartionQueryDto {
        if (quantity <= 0) {
            throw new GenericError("La quantité doit être supérieure à 0");
        }
    }
}
