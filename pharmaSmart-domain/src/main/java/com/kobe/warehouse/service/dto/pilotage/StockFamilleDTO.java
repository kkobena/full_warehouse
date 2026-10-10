package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Stock d'une famille : quantité et valeur au prix d'achat (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record StockFamilleDTO(String cle, String libelle, Long quantite, Long valeur) {}
