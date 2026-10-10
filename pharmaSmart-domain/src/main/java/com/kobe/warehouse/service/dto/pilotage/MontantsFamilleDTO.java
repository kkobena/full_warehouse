package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Deux montants d'une famille (HT et TTC) sur une période (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MontantsFamilleDTO(String cle, String libelle, Long montantHt, Long montantTtc) {}
