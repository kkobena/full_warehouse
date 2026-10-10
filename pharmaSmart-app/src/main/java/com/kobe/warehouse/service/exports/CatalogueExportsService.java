package com.kobe.warehouse.service.exports;

import com.kobe.warehouse.service.dto.exports.CatalogueExportsDTO;

public interface CatalogueExportsService {
    /** Les exports de données ouverts à l'utilisateur courant (droit d'export de la rubrique) et les exports existants qu'il peut ouvrir. */
    CatalogueExportsDTO lireCatalogue();
}
