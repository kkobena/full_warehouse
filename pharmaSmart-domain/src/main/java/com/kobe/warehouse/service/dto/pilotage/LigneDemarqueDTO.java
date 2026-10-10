package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Démarque d'un motif (périmés, casse, vol…), comparée à la référence. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneDemarqueDTO(String libelle, long quantite, CelluleAnalyseDTO valeur) {}
