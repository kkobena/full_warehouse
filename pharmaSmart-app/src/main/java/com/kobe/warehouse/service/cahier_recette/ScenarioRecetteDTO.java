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
    Boolean hidden,
    /** Captures d'écran injectées à la génération ; null tant qu'aucune campagne n'a tourné. */
    List<CaptureEcranDTO> captures
) {}
