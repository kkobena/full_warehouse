package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Facturé et réglé sur les factures d'une période, par organisme (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FactureOrganismeDTO(String cle, String libelle, Long facture, Long regle) {}
