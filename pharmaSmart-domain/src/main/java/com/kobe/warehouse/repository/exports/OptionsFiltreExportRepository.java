package com.kobe.warehouse.repository.exports;

import com.kobe.warehouse.domain.enumeration.SourceOptionsExport;
import com.kobe.warehouse.service.dto.exports.OptionFiltreDTO;
import java.util.List;

/** Valeurs proposées par les filtres d'export dont les options viennent d'un référentiel de l'officine. */
public interface OptionsFiltreExportRepository {
    /** @return les options du référentiel, triées pour l'affichage ; vide si la source porte déjà ses options */
    List<OptionFiltreDTO> lireOptions(SourceOptionsExport source);
}
