package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MembreAnalyseDTO(String cle, String libelle) {}
