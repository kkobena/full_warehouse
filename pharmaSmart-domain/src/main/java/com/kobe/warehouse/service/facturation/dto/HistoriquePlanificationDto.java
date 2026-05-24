package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import java.time.LocalDateTime;

public record HistoriquePlanificationDto(
    Long id,
    Integer planificationId,
    LocalDateTime executionDebut,
    LocalDateTime executionFin,
    ExecutionStatut statut,
    Integer generationCode,
    Integer nombreFactures,
    String message
) {}
