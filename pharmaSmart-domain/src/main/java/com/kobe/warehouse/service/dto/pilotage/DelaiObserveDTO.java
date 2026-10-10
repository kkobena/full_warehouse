package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Délai moyen observé entre la facture et ses règlements, par organisme, et le nombre de factures réglées qui le fondent. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DelaiObserveDTO(String cle, Double delaiMoyen, Long factures) {}
