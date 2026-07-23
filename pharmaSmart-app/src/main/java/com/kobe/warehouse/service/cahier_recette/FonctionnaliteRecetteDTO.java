package com.kobe.warehouse.service.cahier_recette;

import java.util.List;

/** Miroir Java de l'interface TS FonctionnaliteRecette (cahier-recette.model.ts). */
public record FonctionnaliteRecetteDTO(
    String nom,
    String description,
    List<ScenarioRecetteDTO> scenarios,
    Boolean hidden,
    String version,
    Boolean roadmap
) {}
