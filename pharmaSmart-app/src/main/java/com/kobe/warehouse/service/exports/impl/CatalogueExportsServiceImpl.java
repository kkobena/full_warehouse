package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.LienExport;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.dto.exports.CatalogueExportsDTO;
import com.kobe.warehouse.service.dto.exports.ExportCatalogueDTO;
import com.kobe.warehouse.service.dto.exports.LienExportDTO;
import com.kobe.warehouse.service.exports.CatalogueExportsService;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogueExportsServiceImpl implements CatalogueExportsService {

    private final NavAccessService navAccessService;

    public CatalogueExportsServiceImpl(NavAccessService navAccessService) {
        this.navAccessService = navAccessService;
    }

    @Override
    public CatalogueExportsDTO lireCatalogue() {
        List<ExportCatalogueDTO> exports = Arrays.stream(ExportDonnees.values())
            .filter(export -> navAccessService.isAllowed(List.of(export.getRubrique().getDroit()), NavAction.EXPORT))
            .map(export ->
                new ExportCatalogueDTO(
                    export,
                    export.getRubrique(),
                    export.getRubrique().getLibelle(),
                    export.getLibelle(),
                    export.getDescription(),
                    export.isPeriodique(),
                    export.getColonnes().stream().map(ExportDonnees.ColonneExport::libelle).toList()
                )
            )
            .toList();
        List<LienExportDTO> liens = Arrays.stream(LienExport.values())
            .filter(lien -> navAccessService.isAllowed(List.of(lien.getDroit()), NavAction.DISPLAY))
            .map(lien -> new LienExportDTO(lien.getRubrique(), lien.getLibelle(), lien.getDescription(), lien.getRoute(), lien.getOnglet()))
            .toList();
        return new CatalogueExportsDTO(exports, liens);
    }
}
