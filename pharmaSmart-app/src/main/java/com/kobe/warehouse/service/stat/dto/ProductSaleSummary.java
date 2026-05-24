package com.kobe.warehouse.service.stat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record ProductSaleSummary(
    Integer groupType,
    String groupBy,
    Integer montantHt,
    Integer quantite,
    Integer montantAchat,
    Integer montantTtc,
    Integer montantRemise
) {
    @JsonProperty("mvtDate")
    public LocalDate mvtDate() {
        switch (groupType) {
            case 0 -> {
                return LocalDate.parse(groupBy);
            }
            case 1 -> {
                String[] parts = groupBy.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                return LocalDate.of(year, month, 1);
            }
            case 3, 4 -> {
                int year = Integer.parseInt(groupBy);
                return LocalDate.of(year, 1, 1);
            }
            default -> {
                return null;
            }
        }
    }
}
