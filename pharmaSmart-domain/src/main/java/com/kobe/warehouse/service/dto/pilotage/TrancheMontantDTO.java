package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Un montant étiqueté : tranche d'ancienneté, mois d'échéance… */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record TrancheMontantDTO(String libelle, long montant) {}
