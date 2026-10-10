package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AnneeCompareeDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.ElementAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointSerieDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SeriesPilotageDTO;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.ComparaisonAnneesService;
import com.kobe.warehouse.service.pilotage.ExportPilotageService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.FormatTableur;
import com.kobe.warehouse.service.pilotage.calcul.LibellesPilotage;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportPilotageServiceImpl implements ExportPilotageService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String REFERENCE = " (référence)";
    private static final int MOIS_PAR_AN = 12;

    private final SeriesPilotageService seriesPilotageService;
    private final AnalysePilotageService analysePilotageService;
    private final ComparaisonAnneesService comparaisonAnneesService;
    private final CsvExportService csvExportService;

    public ExportPilotageServiceImpl(
        SeriesPilotageService seriesPilotageService,
        AnalysePilotageService analysePilotageService,
        ComparaisonAnneesService comparaisonAnneesService,
        CsvExportService csvExportService
    ) {
        this.seriesPilotageService = seriesPilotageService;
        this.analysePilotageService = analysePilotageService;
        this.comparaisonAnneesService = comparaisonAnneesService;
        this.csvExportService = csvExportService;
    }

    @Override
    public byte[] exporterSeries(RequetePilotageDTO requete) {
        SeriesPilotageDTO donnees = seriesPilotageService.calculerSeries(requete);
        List<SerieIndicateurDTO> series = donnees.series();
        List<String> entete = new ArrayList<>();
        entete.add("Période");
        for (SerieIndicateurDTO serie : series) {
            ajouterColonnes(entete, serie.indicateur());
        }
        int tranches = series.isEmpty() ? 0 : series.getFirst().points().size();
        List<String[]> lignes = new ArrayList<>(tranches);
        for (int rang = tranches - 1; rang >= 0; rang--) {
            List<String> ligne = new ArrayList<>();
            ligne.add(series.getFirst().points().get(rang).libelle());
            for (SerieIndicateurDTO serie : series) {
                PointSerieDTO point = serie.points().get(rang);
                ligne.add(FormatTableur.formater(point.valeur()));
                ligne.add(FormatTableur.formater(point.valeurReference()));
            }
            lignes.add(ligne.toArray(String[]::new));
        }
        return ecrire("Pilotage — détail par période " + libellerPeriode(donnees.comparaison().periode()), entete, lignes);
    }

    @Override
    public byte[] exporterAnalyse(RequetePilotageDTO requete, RequeteAnalyseDTO reglage) {
        AnalysePilotageDTO analyse = analysePilotageService.analyser(requete, reglage);
        List<String> entete = new ArrayList<>();
        entete.add(analyse.axe().libelle());
        for (IndicateurPilotageDTO indicateur : analyse.indicateurs()) {
            ajouterColonnes(entete, indicateur);
        }
        entete.add("Part (%)");
        entete.add("Contribution à l'écart (%)");
        List<String[]> lignes = new ArrayList<>(analyse.elements().size() + 2);
        for (ElementAnalyseDTO element : analyse.elements()) {
            lignes.add(lireElement(element));
        }
        if (analyse.autres() != null) {
            lignes.add(lireElement(analyse.autres()));
        }
        lignes.add(lireLigne("Total", analyse.total(), "100", ""));
        return ecrire("Pilotage — " + analyse.axe().libelle() + " " + libellerPeriode(analyse.comparaison().periode()), entete, lignes);
    }

    @Override
    public byte[] exporterAnnees(RequeteAnneesDTO requete) {
        ComparaisonAnneesDTO comparaison = comparaisonAnneesService.comparerAnnees(requete, LocalDate.now());
        List<String> entete = new ArrayList<>();
        entete.add("Mois");
        for (AnneeCompareeDTO annee : comparaison.annees()) {
            entete.add(annee.complete() ? String.valueOf(annee.annee()) : annee.annee() + " (en cours)");
        }
        LocalDate jusquAu = comparaison.jusquAu();
        List<String[]> lignes = new ArrayList<>(MOIS_PAR_AN + 1);
        for (int mois = 1; mois <= MOIS_PAR_AN; mois++) {
            String libelle = LibellesPilotage.libellerMoisCourt(LocalDate.of(jusquAu.getYear(), mois, 1));
            List<String> ligne = new ArrayList<>();
            ligne.add(mois == jusquAu.getMonthValue() ? libelle + " (au " + jusquAu.getDayOfMonth() + ")" : libelle);
            for (AnneeCompareeDTO annee : comparaison.annees()) {
                ligne.add(FormatTableur.formater(annee.mois().get(mois - 1).valeur()));
            }
            lignes.add(ligne.toArray(String[]::new));
        }
        List<String> total = new ArrayList<>();
        total.add("Année");
        for (AnneeCompareeDTO annee : comparaison.annees()) {
            total.add(FormatTableur.formater(annee.total().valeur()));
        }
        lignes.add(total.toArray(String[]::new));
        return ecrire("Pilotage — " + comparaison.indicateur().libelle() + " par mois et par année", entete, lignes);
    }

    private static void ajouterColonnes(List<String> entete, IndicateurPilotageDTO indicateur) {
        entete.add(indicateur.libelle());
        entete.add(indicateur.libelle() + REFERENCE);
    }

    private static String[] lireElement(ElementAnalyseDTO element) {
        return lireLigne(element.libelle(), element.cellules(), FormatTableur.formater(element.part()), FormatTableur.formater(element.contribution()));
    }

    private static String[] lireLigne(String libelle, List<CelluleAnalyseDTO> cellules, String part, String contribution) {
        List<String> ligne = new ArrayList<>(cellules.size() * 2 + 3);
        ligne.add(libelle);
        for (CelluleAnalyseDTO cellule : cellules) {
            ligne.add(FormatTableur.formater(cellule.valeur()));
            ligne.add(FormatTableur.formater(cellule.valeurReference()));
        }
        ligne.add(part);
        ligne.add(contribution);
        return ligne.toArray(String[]::new);
    }

    private static String libellerPeriode(PeriodeDTO periode) {
        return "du " + periode.du().format(JOUR) + " au " + periode.au().format(JOUR);
    }

    private byte[] ecrire(String titre, List<String> entete, List<String[]> lignes) {
        try {
            return csvExportService.addUtf8Bom(csvExportService.createSimpleCsvReport(titre, entete.toArray(String[]::new), lignes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
