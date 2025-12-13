package com.kobe.warehouse.service.dto.dashboard;

public record PeremptionsDTO(
    Integer unMois,
    Integer unATroisMois,
    Integer troisASixMois,
    Long valeurTotale
) {
}
