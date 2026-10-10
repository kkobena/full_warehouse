package com.kobe.warehouse.service.dto.exports;

import java.util.List;

/** Ce que l'utilisateur courant peut exporter : les exports de données, puis les exports existants rattachés. */
public record CatalogueExportsDTO(List<ExportCatalogueDTO> exports, List<LienExportDTO> liens) {}
