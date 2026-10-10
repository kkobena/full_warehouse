package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Reste dû des ventes différées par ancienneté (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DifferesAgeDTO(Long moins30, Long de31a60, Long de61a90, Long plus90) {}
