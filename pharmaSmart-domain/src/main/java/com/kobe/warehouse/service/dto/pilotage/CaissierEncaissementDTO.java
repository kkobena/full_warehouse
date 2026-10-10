package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Encaissé et sessions de caisse d'un caissier. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CaissierEncaissementDTO(String cle, String libelle, long montant, long transactions, long sessions) {}
