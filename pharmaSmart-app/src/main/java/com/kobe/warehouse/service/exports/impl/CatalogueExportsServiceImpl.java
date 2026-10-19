package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.SourceOptionsExport;
import com.kobe.warehouse.domain.enumeration.SujetExport;
import com.kobe.warehouse.domain.enumeration.SujetsExport;
import com.kobe.warehouse.repository.exports.OptionsFiltreExportRepository;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.dto.exports.CatalogueExportsDTO;
import com.kobe.warehouse.service.dto.exports.ExportCatalogueDTO;
import com.kobe.warehouse.service.dto.exports.FiltreCatalogueDTO;
import com.kobe.warehouse.service.dto.exports.OptionFiltreDTO;
import com.kobe.warehouse.service.exports.CatalogueExportsService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le catalogue ouvert à l'utilisateur, réglages compris : chaque entrée annonce ses filtres avec les valeurs où choisir, afin
 * que l'écran des exports produise ici ce qu'on serait allé chercher dans l'écran des données. Les référentiels ne sont lus
 * qu'une fois par appel, même quand plusieurs entrées partagent un filtre.
 *
 * <p>Tout se produit ici, sans aller nulle part : les éditions des autres écrans (comptabilité, déclarations, pilotage) sont
 * bâties sur place par le service de leur écran. Les boutons d'export de ces écrans, eux, ne bougent pas : le menu Exports est
 * un chemin de plus, jamais un remplacement.
 */
@Service
@Transactional(readOnly = true)
public class CatalogueExportsServiceImpl implements CatalogueExportsService {

    private final NavAccessService navAccessService;
    private final OptionsFiltreExportRepository optionsFiltreExportRepository;

    public CatalogueExportsServiceImpl(NavAccessService navAccessService, OptionsFiltreExportRepository optionsFiltreExportRepository) {
        this.navAccessService = navAccessService;
        this.optionsFiltreExportRepository = optionsFiltreExportRepository;
    }

    @Override
    public CatalogueExportsDTO lireCatalogue() {
        Map<SourceOptionsExport, List<OptionFiltreDTO>> referentiels = new EnumMap<>(SourceOptionsExport.class);
        List<ExportCatalogueDTO> exports = new ArrayList<>();
        for (SujetExport sujet : SujetsExport.lister()) {
            if (navAccessService.isAllowed(sujet.getDroits(), NavAction.EXPORT)) {
                exports.add(decrire(sujet, referentiels));
            }
        }
        return new CatalogueExportsDTO(exports);
    }

    private ExportCatalogueDTO decrire(SujetExport sujet, Map<SourceOptionsExport, List<OptionFiltreDTO>> referentiels) {
        return new ExportCatalogueDTO(
            sujet.name(),
            sujet.getLibelleRubrique(),
            sujet.getLibelle(),
            sujet.getDescription(),
            sujet.isPeriodique(),
            sujet.estProgrammable(),
            sujet.getFormats(),
            lireColonnes(sujet),
            lireFiltres(sujet, referentiels)
        );
    }

    /** Une édition est déjà mise en forme par son service : elle n'annonce pas de colonnes. */
    private static List<String> lireColonnes(SujetExport sujet) {
        if (!(sujet instanceof ExportDonnees donnees)) {
            return List.of();
        }
        List<String> colonnes = new ArrayList<>();
        for (ExportDonnees.ColonneExport colonne : donnees.getColonnes()) {
            colonnes.add(colonne.libelle());
        }
        return colonnes;
    }

    private List<FiltreCatalogueDTO> lireFiltres(SujetExport sujet, Map<SourceOptionsExport, List<OptionFiltreDTO>> referentiels) {
        List<FiltreCatalogueDTO> filtres = new ArrayList<>();
        for (FiltreExport filtre : sujet.getFiltres()) {
            filtres.add(
                new FiltreCatalogueDTO(filtre, filtre.getLibelle(), filtre.estMultiple(), lireOptions(filtre.getSource(), referentiels))
            );
        }
        return filtres;
    }

    private List<OptionFiltreDTO> lireOptions(SourceOptionsExport source, Map<SourceOptionsExport, List<OptionFiltreDTO>> referentiels) {
        if (!source.estReferentiel()) {
            return source.getOptions();
        }
        return referentiels.computeIfAbsent(source, optionsFiltreExportRepository::lireOptions);
    }
}
