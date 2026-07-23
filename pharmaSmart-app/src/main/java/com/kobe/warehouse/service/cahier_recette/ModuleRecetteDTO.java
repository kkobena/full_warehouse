package com.kobe.warehouse.service.cahier_recette;

import java.util.List;

/** Miroir Java de l'interface TS ModuleRecette (cahier-recette.model.ts). */
public record ModuleRecetteDTO(String id, String nom, String icone, String description, List<FonctionnaliteRecetteDTO> fonctionnalites) {}
