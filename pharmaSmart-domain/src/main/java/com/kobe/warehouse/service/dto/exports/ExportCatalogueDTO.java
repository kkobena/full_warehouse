package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.FormatExport;
import java.util.List;

/**
 * Une entrée du catalogue, telle que l'écran la présente et la règle.
 *
 * @param code nom de la constante, ce que la demande renverra
 * @param colonnes colonnes de l'export de données ; vide pour une édition, déjà mise en forme par son service
 * @param programmable enregistrable dans un modèle programmé
 */
public record ExportCatalogueDTO(
    String code,
    String libelleRubrique,
    String libelle,
    String description,
    boolean periodique,
    boolean programmable,
    List<FormatExport> formats,
    List<String> colonnes,
    List<FiltreCatalogueDTO> filtres
) {}
