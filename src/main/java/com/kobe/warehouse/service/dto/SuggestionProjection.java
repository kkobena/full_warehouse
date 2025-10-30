package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import java.time.LocalDateTime;

public record SuggestionProjection(
    Long id,
    String suggessionReference,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    TypeSuggession typeSuggession,
    StatutSuggession statut,
    long fournisseurId,
    String fournisseurLibelle
) {
    @JsonProperty("statutLibelle")
    public String statutLibelle() {
        return statut.getLibelle();
    }
}
