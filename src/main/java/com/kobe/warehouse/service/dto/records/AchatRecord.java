package com.kobe.warehouse.service.dto.records;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record AchatRecord(
    Long receiptAmount,
    Long discountAmount,
    Long taxAmount,
    Long achatCount
) {
    @JsonProperty("ttcAmount")
    public long ttcAmount() {
        return Objects.requireNonNullElse(receiptAmount, 0L) + Objects.requireNonNullElse(taxAmount, 0L);
    }
    @JsonProperty("netAmount")
    public long netAmount(){
        return Objects.requireNonNullElse(receiptAmount, 0L)-Objects.requireNonNullElse(discountAmount, 0L);
    }
}
