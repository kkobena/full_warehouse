package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AffichageAnalyse;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ElementAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.ReglageAnalyseRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.AnalyserRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO.StyleSerie;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.GraphiquePilotageService;
import com.kobe.warehouse.service.pilotage.RapportAnalyserService;
import com.kobe.warehouse.service.pilotage.calcul.FormatRapport;
import com.kobe.warehouse.service.pilotage.calcul.HorizonCourbe;
import com.kobe.warehouse.service.pilotage.calcul.TableauxRapport;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RapportAnalyserServiceImpl implements RapportAnalyserService {

    /** Au-delà, une courbe ne se lit plus : même borne que l'écran. */
    private static final int SERIES_MAX_COURBE = 8;
    /** Au-delà, les barres dépasseraient la page ; le détail, lui, reprend tous les éléments. */
    private static final int BARRES_MAX = 25;
    private static final String CENT_POUR_CENT = "100,0 %";

    private final AnalysePilotageService analysePilotageService;
    private final GraphiquePilotageService graphiquePilotageService;

    public RapportAnalyserServiceImpl(AnalysePilotageService analysePilotageService, GraphiquePilotageService graphiquePilotageService) {
        this.analysePilotageService = analysePilotageService;
        this.graphiquePilotageService = graphiquePilotageService;
    }

    @Override
    public AnalyserRapportDTO construireAnalyse(RequetePilotageDTO requete, ReglageAnalyseRapportDTO reglage) {
        RequetePilotageDTO requeteIndicateurs = requete.lireAvecIndicateurs(reglage.indicateurs());
        AnalysePilotageDTO analyse = analysePilotageService.analyser(requeteIndicateurs, reglage.analyse());
        IndicateurPilotageDTO premier = analyse.indicateurs().getFirst();
        String axe = analyse.axe().libelle().toLowerCase();
        AffichageAnalyse affichage = reglage.affichage();
        return new AnalyserRapportDTO(
            decrireReglage(analyse, reglage.analyse()),
            libellerChemin(reglage.libellesChemin()),
            libellerIgnores(analyse),
            switch (affichage) {
                case BARRES -> premier.libelle() + " par " + axe + (analyse.elements().size() > BARRES_MAX ? " (les " + BARRES_MAX + " premiers)" : "");
                case COURBE -> premier.libelle() + " — " + HorizonCourbe.libellerHorizon(requete.granularite());
                case CROISE -> premier.libelle() + " : tableau croisé";
                case TABLEAU -> null;
            },
            switch (affichage) {
                case BARRES -> dessinerClassement(analyse);
                case COURBE -> dessinerCourbes(requete, reglage);
                case CROISE, TABLEAU -> null;
            },
            affichage == AffichageAnalyse.CROISE ? lireCroise(analyse) : null,
            "Détail par " + axe,
            construireDetail(analyse)
        );
    }

    /** « CA TTC, Marge brute par famille, puis par laboratoire ; 20 premiers éléments, triés par valeur. » */
    private static String decrireReglage(AnalysePilotageDTO analyse, RequeteAnalyseDTO reglage) {
        StringBuilder texte = new StringBuilder(String.join(", ", analyse.indicateurs().stream().map(IndicateurPilotageDTO::libelle).toList()))
            .append(" par ")
            .append(analyse.axe().libelle().toLowerCase());
        if (analyse.axe2() != null) {
            texte.append(", puis par ").append(analyse.axe2().libelle().toLowerCase());
        }
        texte.append(reglage.top() > 0 ? " ; " + reglage.top() + " premiers éléments" : " ; tous les éléments");
        texte.append(
            switch (reglage.tri()) {
                case VALEUR -> ", triés par valeur.";
                case ECART_HAUSSE -> ", plus fortes hausses d'abord.";
                case ECART_BAISSE -> ", plus fortes baisses d'abord.";
            }
        );
        return texte.toString();
    }

    /** « Tout › Antalgiques › Doliprane 1000 », comme le fil de l'écran. */
    private static String libellerChemin(List<String> libelles) {
        if (libelles.isEmpty()) {
            return null;
        }
        return "Tout › " + String.join(" › ", libelles);
    }

    private static String libellerIgnores(AnalysePilotageDTO analyse) {
        if (analyse.indicateursIgnores().isEmpty()) {
            return null;
        }
        List<String> libelles = analyse.indicateursIgnores().stream().map(IndicateurPilotageDTO::libelle).toList();
        return "Non ventilable par " + analyse.axe().libelle().toLowerCase() + " : " + String.join(", ", libelles) + ".";
    }

    /** La période en plein, la référence pâlie à côté ; sur le premier indicateur, comme l'écran. */
    private String dessinerClassement(AnalysePilotageDTO analyse) {
        if (analyse.elements().isEmpty()) {
            return null;
        }
        List<ElementAnalyseDTO> elements = analyse.elements().subList(0, Math.min(BARRES_MAX, analyse.elements().size()));
        List<String> libelles = new ArrayList<>(elements.size());
        List<Double> periode = new ArrayList<>(elements.size());
        List<Double> reference = new ArrayList<>(elements.size());
        for (ElementAnalyseDTO element : elements) {
            libelles.add(element.libelle());
            periode.add(element.cellules().getFirst().valeur());
            reference.add(element.cellules().getFirst().valeurReference());
        }
        PeriodeDTO comparee = analyse.comparaison().reference();
        List<SerieGraphiqueDTO> series = new ArrayList<>(2);
        series.add(new SerieGraphiqueDTO("Période : " + libellerPeriode(analyse.comparaison().periode()), periode, StyleSerie.REALISE));
        if (comparee != null) {
            series.add(new SerieGraphiqueDTO("Comparée à : " + libellerPeriode(comparee), reference, StyleSerie.FANTOME));
        }
        return graphiquePilotageService.dessinerClassement(libelles, series, analyse.indicateurs().getFirst().unite());
    }

    /**
     * Les éléments dans le temps, sur les 13 dernières tranches : le second axe devient la période et le nombre de courbes est
     * borné, comme {@code preparerRequete} de l'écran.
     */
    private String dessinerCourbes(RequetePilotageDTO requete, ReglageAnalyseRapportDTO reglage) {
        RequeteAnalyseDTO demande = reglage.analyse();
        int top = demande.top() > 0 ? Math.min(demande.top(), SERIES_MAX_COURBE) : SERIES_MAX_COURBE;
        AxeAnalyse axe2 = demande.axe() == AxeAnalyse.PERIODE ? null : AxeAnalyse.PERIODE;
        RequeteAnalyseDTO dansLeTemps = new RequeteAnalyseDTO(demande.axe(), axe2, top, demande.tri(), demande.filtre());
        RequetePilotageDTO historique = HorizonCourbe.elargirAuxTreizeTranches(requete, reglage.indicateurs().getFirst()).lireAvecIndicateurs(
            reglage.indicateurs()
        );
        AnalysePilotageDTO evolution = analysePilotageService.analyser(historique, dansLeTemps);
        CroiseAnalyseDTO croise = evolution.croise();
        if (croise == null || croise.lignes().isEmpty()) {
            return null;
        }
        List<SerieGraphiqueDTO> series = new ArrayList<>(croise.lignes().size());
        for (LigneCroiseeDTO ligne : croise.lignes()) {
            List<Double> valeurs = ligne.cellules().stream().map(cellule -> cellule == null ? null : cellule.valeur()).toList();
            series.add(new SerieGraphiqueDTO(ligne.libelle(), valeurs, StyleSerie.PALETTE));
        }
        List<String> tranches = croise.colonnes().stream().map(MembreAnalyseDTO::libelle).toList();
        return graphiquePilotageService.dessinerCourbes(tranches, series, evolution.indicateurs().getFirst().unite(), FormatGraphe.LARGE);
    }

    /** Vide sans second axe : le gabarit invite alors à en choisir un, comme l'écran. */
    private static TableauRapportDTO lireCroise(AnalysePilotageDTO analyse) {
        if (analyse.croise() == null) {
            return new TableauRapportDTO(analyse.axe().libelle(), List.of(), List.of());
        }
        String entete = analyse.axe().libelle() + " / " + analyse.axe2().libelle();
        boolean avecReference = analyse.comparaison().reference() != null;
        return TableauxRapport.lireCroise(entete, analyse.croise(), analyse.indicateurs().getFirst(), avecReference);
    }

    /** Pendant d'{@code app-tableau-analyse} : éléments, « autres », puis le total ; part et contribution sur le premier indicateur. */
    private static TableauRapportDTO construireDetail(AnalysePilotageDTO analyse) {
        boolean avecPart = false;
        boolean avecContribution = false;
        for (ElementAnalyseDTO element : analyse.elements()) {
            avecPart |= element.part() != null;
            avecContribution |= element.contribution() != null;
        }
        List<String> colonnes = new ArrayList<>(analyse.indicateurs().stream().map(IndicateurPilotageDTO::libelle).toList());
        if (avecPart) {
            colonnes.add("Part");
        }
        if (avecContribution) {
            colonnes.add("Contribution à l'écart");
        }
        List<LigneRapportDTO> lignes = new ArrayList<>(analyse.elements().size() + 2);
        for (ElementAnalyseDTO element : analyse.elements()) {
            lignes.add(lireElement(analyse, element, avecPart, avecContribution));
        }
        if (analyse.autres() != null) {
            lignes.add(lireElement(analyse, analyse.autres(), avecPart, avecContribution));
        }
        List<CelluleRapportDTO> totaux = new ArrayList<>(TableauxRapport.lireCellules(analyse.total(), analyse.indicateurs()));
        if (avecPart) {
            totaux.add(new CelluleRapportDTO(CENT_POUR_CENT, null));
        }
        if (avecContribution) {
            totaux.add(new CelluleRapportDTO(CENT_POUR_CENT, null));
        }
        lignes.add(new LigneRapportDTO("Total (" + analyse.nombreElements() + ")", totaux, true));
        return new TableauRapportDTO(analyse.axe().libelle(), colonnes, lignes);
    }

    private static LigneRapportDTO lireElement(AnalysePilotageDTO analyse, ElementAnalyseDTO element, boolean avecPart, boolean avecContribution) {
        List<CelluleRapportDTO> cellules = new ArrayList<>(TableauxRapport.lireCellules(element.cellules(), analyse.indicateurs()));
        if (avecPart) {
            cellules.add(TableauxRapport.lireValeur(element.part(), UniteIndicateur.POURCENTAGE));
        }
        if (avecContribution) {
            cellules.add(TableauxRapport.lireValeur(element.contribution(), UniteIndicateur.POURCENTAGE));
        }
        return new LigneRapportDTO(element.libelle(), cellules);
    }

    private static String libellerPeriode(PeriodeDTO periode) {
        return "du " + FormatRapport.formaterDate(periode.du()) + " au " + FormatRapport.formaterDate(periode.au());
    }
}
