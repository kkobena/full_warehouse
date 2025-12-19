package com.kobe.warehouse.service.reassort.dto;

import com.kobe.warehouse.service.errors.GenericError;

public record RepartionQueryDto(Integer stockSourceId, Integer stockDestinationId, int quantity, Integer seuilMini) {
    public RepartionQueryDto {
        if (quantity <= 0) {
            throw new GenericError("La quantité doit être supérieure à 0");
        }
    }
}
