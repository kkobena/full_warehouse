package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Achats et coût des ventes d'une famille (HT) ; ratio = coût des ventes / achats. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneAchatsVentesDTO(String cle, String libelle, long achatsHt, long coutVentesHt, long ecart, Double ratio) {}
