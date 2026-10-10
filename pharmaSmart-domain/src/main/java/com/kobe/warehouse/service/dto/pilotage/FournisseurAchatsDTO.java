package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Achats reçus d'un fournisseur sur une période (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FournisseurAchatsDTO(String cle, String libelle, Long montantTtc, Long nbBons, Long quantiteCommandee, Long quantiteRecue) {}
