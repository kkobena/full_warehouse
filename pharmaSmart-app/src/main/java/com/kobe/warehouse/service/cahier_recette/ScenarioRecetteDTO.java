package com.kobe.warehouse.service.cahier_recette;

import java.util.List;

/** Miroir Java de l'interface TS ScenarioRecette (cahier-recette.model.ts). */
public record ScenarioRecetteDTO(
    String id,
    String titre,
    String besoin,
    String fonctionnement,
    String prerequis,
    List<String> etapes,
    String resultatAttendu,
    Boolean hidden
) {}
