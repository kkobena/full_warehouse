package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import java.util.List;

/**
 * Un réglage proposé par une entrée du catalogue, avec les valeurs où choisir.
 *
 * @param multiple plusieurs valeurs à la fois ; sinon l'écran n'en propose qu'une
 */
public record FiltreCatalogueDTO(FiltreExport code, String libelle, boolean multiple, List<OptionFiltreDTO> options) {}
