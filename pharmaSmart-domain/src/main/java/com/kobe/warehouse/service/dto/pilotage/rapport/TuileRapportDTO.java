package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/** Tuile d'indicateur : valeur, variation et sous-lignes (indicateur qui l'éclaire, objectif, projection). */
public record TuileRapportDTO(String libelle, String valeur, VariationRapportDTO variation, List<String> complements) {}
