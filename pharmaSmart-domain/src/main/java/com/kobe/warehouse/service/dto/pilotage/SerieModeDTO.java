package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Encaissé par un mode, tranche après tranche (graphique empilé). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SerieModeDTO(String libelle, List<Long> valeurs) {}
