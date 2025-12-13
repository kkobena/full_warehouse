package com.kobe.warehouse.service.reassort.dto;

public record ManualRepartitionRequest(
    Integer stockSourceId,
    Integer stockDestinationId,
    Integer quantity
) {
}
