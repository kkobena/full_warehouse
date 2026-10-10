package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record SeriesPilotageDTO(ComparaisonPeriodesDTO comparaison, long joursOuvres, long joursOuvresReference, List<SerieIndicateurDTO> series) {}
