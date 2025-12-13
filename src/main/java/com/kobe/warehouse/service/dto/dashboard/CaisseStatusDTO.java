package com.kobe.warehouse.service.dto.dashboard;

import java.time.LocalDateTime;

public record CaisseStatusDTO(
    Long soldeOuverture,
    Long soldeActuel,
    Long soldeAttendu,
    Long ecart,
    LocalDateTime derniereFermeture,
    String etat // OUVERTE or FERMEE
) {}
