package com.kobe.warehouse.service.cahier_recette;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Miroir Java de l'interface TS ModuleRecette (cahier-recette.model.ts). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModuleRecetteDTO(
        String id,
        String nom,
        String icone,
        String description,
        List<FonctionnaliteRecetteDTO> fonctionnalites
) {}
