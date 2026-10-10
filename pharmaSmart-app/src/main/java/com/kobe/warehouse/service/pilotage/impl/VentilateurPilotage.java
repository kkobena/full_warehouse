package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.repository.PilotageAnalyseRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MesuresVentileesDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteVentilationDTO;
import com.kobe.warehouse.service.pilotage.calcul.LectureVentilation;
import com.kobe.warehouse.service.pilotage.calcul.LibellesPilotage;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import com.kobe.warehouse.service.pilotage.calcul.TranchesPeriode;
import com.kobe.warehouse.service.pilotage.calcul.TranchesRemise;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ventile la période et sa référence par un ou deux axes et rapproche les éléments : un seul passage par ligne lue. Les axes
 * temporels se déduisent du jour : la période, par rang de tranche (la tranche N de la période face à la tranche N de la
 * référence) ; le jour de la semaine, par son numéro ISO (1 = lundi).
 */
@Component
@Transactional(readOnly = true)
class VentilateurPilotage {

    private static final Locale FRANCAIS = Locale.FRENCH;

    private final PilotageAnalyseRepository pilotageAnalyseRepository;
    private final AppConfigurationService appConfigurationService;

    VentilateurPilotage(PilotageAnalyseRepository pilotageAnalyseRepository, AppConfigurationService appConfigurationService) {
        this.pilotageAnalyseRepository = pilotageAnalyseRepository;
        this.appConfigurationService = appConfigurationService;
    }

    /** @param axe {@code null} : un seul élément, le total du périmètre filtré */
    Ventilation comparer(
        SourceAnalyse source,
        ComparaisonPeriodesDTO comparaison,
        AxeAnalyse axe,
        AxeAnalyse axe2,
        List<FiltreAnalyseDTO> filtres,
        Granularite granularite
    ) {
        List<AxeAnalyse> axes = Stream.of(axe, axe2).filter(Objects::nonNull).toList();
        TranchesRemise tranchesRemise = axes.contains(AxeAnalyse.REMISE)
            ? TranchesRemise.decouper(appConfigurationService.getTranchesRemisePilotage())
            : null;
        LectureVentilation lecture = new LectureVentilation(source, axes, filtres, granularite, tranchesRemise);
        Map<List<String>, MesuresComparees> parCles = new LinkedHashMap<>();
        MesuresComparees total = ajouter(parCles, lecture, comparaison.periode(), true);
        if (comparaison.reference() != null) {
            total = total.plus(ajouter(parCles, lecture, comparaison.reference(), false));
        }
        return new Ventilation(new ArrayList<>(parCles.values()), total);
    }

    /** @return le total des lignes ajoutées */
    private MesuresComparees ajouter(Map<List<String>, MesuresComparees> parCles, LectureVentilation lecture, PeriodeDTO periode, boolean estLaPeriode) {
        List<AxeAnalyse> axes = lecture.axes();
        List<AxeAnalyse> lusEnBase = axes.stream().filter(AxeAnalyse::estLuEnBase).toList();
        boolean parJour = lusEnBase.size() < axes.size();
        TranchesPeriode tranches = TranchesPeriode.decouper(periode, lecture.granularite());
        List<MesuresVentileesDTO> lignes = pilotageAnalyseRepository.ventiler(
            new RequeteVentilationDTO(periode.du(), periode.au(), lecture.source(), lusEnBase, parJour, lecture.filtres(), CategorieChiffreAffaire.officine())
        );
        MesuresComparees total = MesuresComparees.AUCUNES;
        for (MesuresVentileesDTO ligne : lignes) {
            MembreAnalyseDTO membre = axes.isEmpty() ? null : lireMembre(axes.getFirst(), ligne, 0, tranches, lecture.tranchesRemise());
            MembreAnalyseDTO membre2 = axes.size() < 2
                ? null
                : lireMembre(axes.get(1), ligne, lusEnBase.indexOf(axes.get(1)), tranches, lecture.tranchesRemise());
            MesuresPilotage mesures = MesuresPilotage.deVentilation(lecture.source(), ligne);
            MesuresComparees comparees = estLaPeriode
                ? new MesuresComparees(membre, membre2, mesures, MesuresPilotage.AUCUNE)
                : new MesuresComparees(membre, membre2, MesuresPilotage.AUCUNE, mesures);
            parCles.merge(cles(membre, membre2), comparees, MesuresComparees::plus);
            total = total.plus(new MesuresComparees(null, null, comparees.periode(), comparees.reference()));
        }
        return total;
    }

    private static MembreAnalyseDTO lireMembre(
        AxeAnalyse axe,
        MesuresVentileesDTO ligne,
        int rangEnBase,
        TranchesPeriode tranches,
        TranchesRemise tranchesRemise
    ) {
        String cle = rangEnBase == 0 ? ligne.cle1() : ligne.cle2();
        return switch (axe) {
            case PERIODE -> tranches.lireMembre(ligne.jour());
            case JOUR_SEMAINE -> new MembreAnalyseDTO(
                String.valueOf(ligne.jour().getDayOfWeek().getValue()),
                LibellesPilotage.majuscule(ligne.jour().getDayOfWeek().getDisplayName(TextStyle.FULL, FRANCAIS))
            );
            case REMISE -> tranchesRemise.classer(Integer.parseInt(cle));
            default -> {
                String libelle = rangEnBase == 0 ? ligne.libelle1() : ligne.libelle2();
                yield new MembreAnalyseDTO(cle == null ? "" : cle, axe.formaterLibelle(libelle));
            }
        };
    }

    private static List<String> cles(MembreAnalyseDTO membre, MembreAnalyseDTO membre2) {
        return List.of(membre == null ? "" : membre.cle(), membre2 == null ? "" : membre2.cle());
    }

}
