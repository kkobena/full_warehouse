package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.RubriqueExport;
import java.util.List;

public record ExportCatalogueDTO(
    ExportDonnees code,
    RubriqueExport rubrique,
    String libelleRubrique,
    String libelle,
    String description,
    boolean periodique,
    List<String> colonnes
) {}
