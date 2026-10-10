package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Encaissé par un mode de paiement, comparé à la référence ; part du total encaissé (%). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ModeEncaissementDTO(String cle, String libelle, CelluleAnalyseDTO montant, Double part) {}
