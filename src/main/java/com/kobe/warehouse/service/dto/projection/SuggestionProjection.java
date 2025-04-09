package com.kobe.warehouse.service.dto.projection;

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
    public String statutLibelle() {
        return statut.getLibelle();
    }
}
