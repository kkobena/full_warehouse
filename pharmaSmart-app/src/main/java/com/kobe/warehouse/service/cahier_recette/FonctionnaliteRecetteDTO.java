package com.kobe.warehouse.service.cahier_recette;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Miroir Java de l'interface TS FonctionnaliteRecette (cahier-recette.model.ts). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FonctionnaliteRecetteDTO(
    String nom,
    String description,
    /**
     * Chemin d'accès affiché, ex. « Barre de navigation ▸ Gestion Courante ▸ Ventes ».
     * <p>
     * Dérivé de {@code accesCode} par {@link CahierRecetteDataService}. Le modèle ne le
     * renseigne directement que pour les points d'entrée étrangers au menu.
     */
    String acces,
    /** Code {@code nav_item} du menu où se trouve la fonctionnalité, ex. « ventes.devis ». */
    String accesCode,
    /**
     * Suite du chemin à l'intérieur de l'écran, ex. « bouton « Nouvelle vente » ▸ onglet
     * « Assurance » ». Ces éléments n'appartiennent pas au menu : ils restent littéraux.
     */
    String accesSuffixe,
    List<ScenarioRecetteDTO> scenarios,
    Boolean hidden,
    String version,
    Boolean roadmap
) {}
