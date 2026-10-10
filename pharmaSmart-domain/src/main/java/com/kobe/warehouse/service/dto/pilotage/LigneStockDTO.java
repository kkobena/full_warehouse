package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Stock d'une famille en fin de mois.
 *
 * @param rotation coût des ventes TTC des 12 derniers mois / valeur du stock (fois par an)
 * @param couvertureJours valeur du stock / coût des ventes TTC moyen par jour
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneStockDTO(String cle, String libelle, long valeur, Double part, long coutVentes12Mois, Double rotation, Double couvertureJours, long valeurDormante) {}
