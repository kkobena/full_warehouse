package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.AnneeCompareeDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.MoisRecordDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.SyntheseAnneeDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.ComparerAnneesRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO.StyleSerie;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import com.kobe.warehouse.service.pilotage.ComparaisonAnneesService;
import com.kobe.warehouse.service.pilotage.GraphiquePilotageService;
import com.kobe.warehouse.service.pilotage.RapportComparerAnneesService;
import com.kobe.warehouse.service.pilotage.calcul.FormatRapport;
import com.kobe.warehouse.service.pilotage.calcul.LibellesPilotage;
import com.kobe.warehouse.service.pilotage.calcul.TableauxRapport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RapportComparerAnneesServiceImpl implements RapportComparerAnneesService {

    private static final int MOIS_PAR_AN = 12;
    private static final int LIGNES_CROISEES = 30;
    private static final List<String> TRIMESTRES = List.of("T1", "T2", "T3", "T4");
    private static final DateTimeFormatter MOIS_LONG = DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH);
    private static final List<String> COLONNES_SYNTHESE = List.of(
        "CA TTC",
        "Croissance",
        "Marge brute",
        "Taux de marge",
        "Ventes",
        "Panier moyen",
        "Remises"
    );

    private final ComparaisonAnneesService comparaisonAnneesService;
    private final GraphiquePilotageService graphiquePilotageService;

    public RapportComparerAnneesServiceImpl(ComparaisonAnneesService comparaisonAnneesService, GraphiquePilotageService graphiquePilotageService) {
        this.comparaisonAnneesService = comparaisonAnneesService;
        this.graphiquePilotageService = graphiquePilotageService;
    }

    @Override
    public ComparerAnneesRapportDTO construireComparaison(RequeteAnneesDTO reglage, String libelleFiltre) {
        ComparaisonAnneesDTO comparaison = comparaisonAnneesService.comparerAnnees(reglage, LocalDate.now());
        IndicateurPilotageDTO indicateur = comparaison.indicateur();
        List<String> mois = lireMois(comparaison.jusquAu().getYear());
        return new ComparerAnneesRapportDTO(
            decrireReglage(reglage, comparaison, libelleFiltre),
            lireLecture(comparaison),
            indicateur.libelle() + " : une courbe par année",
            graphiquePilotageService.dessinerCourbes(mois, lireSeriesAnnees(comparaison, AnneeCompareeDTO::mois), indicateur.unite(), FormatGraphe.LARGE),
            graphiquePilotageService.dessinerBarres(TRIMESTRES, lireSeriesAnnees(comparaison, AnneeCompareeDTO::trimestres), indicateur.unite(), FormatGraphe.MOITIE),
            dessinerSaisonnalite(comparaison.saisonnalite(), mois),
            construireMoisAnnees(comparaison, mois),
            construireSynthese(comparaison.synthese()),
            lireCroise("Famille / Année", comparaison.parFamille(), indicateur),
            lireCroise("Type de vente / Année", comparaison.parNatureVente(), indicateur)
        );
    }

    private static List<String> lireMois(int annee) {
        List<String> mois = new ArrayList<>(MOIS_PAR_AN);
        for (int rang = 1; rang <= MOIS_PAR_AN; rang++) {
            mois.add(LibellesPilotage.libellerMoisCourt(LocalDate.of(annee, rang, 1)));
        }
        return mois;
    }

    /** « CA TTC sur 3 années civiles, mois par mois, par jour ouvré, filtré sur Antalgiques. » */
    private static String decrireReglage(RequeteAnneesDTO reglage, ComparaisonAnneesDTO comparaison, String libelleFiltre) {
        StringBuilder texte = new StringBuilder(comparaison.indicateur().libelle())
            .append(" sur ")
            .append(comparaison.annees().size())
            .append(" années civiles, ")
            .append(
                switch (comparaison.mode()) {
                    case MENSUEL -> "mois par mois";
                    case CUMULE -> "en cumul depuis janvier";
                    case GLISSANT -> "sur 12 mois glissants";
                }
            );
        if (comparaison.parJourOuvre()) {
            texte.append(", par jour ouvré");
        }
        if (!reglage.filtre().isEmpty()) {
            texte.append(", filtré sur ").append(libelleFiltre == null ? reglage.filtre().getFirst() : libelleFiltre);
        }
        texte.append(". L'année en cours est arrêtée au ").append(FormatRapport.formaterDate(comparaison.jusquAu())).append('.');
        if (reglage.parJourOuvre() && !comparaison.parJourOuvre()) {
            texte.append(" « Par jour ouvré » ne s'applique pas à un taux ou à un ratio.");
        }
        return texte.toString();
    }

    /** Une phrase de lecture : le mois le plus élevé, la croissance annuelle moyenne. */
    private static String lireLecture(ComparaisonAnneesDTO comparaison) {
        List<String> phrases = new ArrayList<>(2);
        MoisRecordDTO maximum = comparaison.moisMaximum();
        if (maximum != null) {
            String mois = LocalDate.of(maximum.annee(), maximum.mois(), 1).format(MOIS_LONG);
            String valeur = FormatRapport.formaterValeur(maximum.valeur(), comparaison.indicateur().unite());
            phrases.add("Mois le plus élevé : " + mois + " " + maximum.annee() + " (" + valeur + ").");
        }
        if (comparaison.croissanceAnnuelleMoyenne() != null) {
            String croissance = FormatRapport.formaterVariation(null, comparaison.croissanceAnnuelleMoyenne(), UniteIndicateur.MONTANT, SensFavorable.HAUSSE).texte();
            phrases.add(
                "Croissance annuelle moyenne " + comparaison.croissanceDepuis() + "-" + comparaison.croissanceJusqua() + " : " + croissance + "."
            );
        }
        return phrases.isEmpty() ? null : String.join(" ", phrases);
    }

    /** L'année la plus récente d'abord, à la couleur d'accent et en trait plein ; les autres en pointillés. */
    private static List<SerieGraphiqueDTO> lireSeriesAnnees(ComparaisonAnneesDTO comparaison, Function<AnneeCompareeDTO, List<CelluleAnalyseDTO>> cellules) {
        List<AnneeCompareeDTO> annees = comparaison.annees();
        List<SerieGraphiqueDTO> series = new ArrayList<>(annees.size());
        for (int rang = annees.size() - 1; rang >= 0; rang--) {
            AnneeCompareeDTO annee = annees.get(rang);
            List<Double> valeurs = cellules.apply(annee).stream().map(cellule -> cellule == null ? null : cellule.valeur()).toList();
            StyleSerie style = rang == annees.size() - 1 ? StyleSerie.PALETTE : StyleSerie.PALETTE_POINTILLE;
            series.add(new SerieGraphiqueDTO(libellerAnnee(annee), valeurs, style));
        }
        return series;
    }

    private String dessinerSaisonnalite(List<Double> saisonnalite, List<String> mois) {
        if (saisonnalite.isEmpty()) {
            return null;
        }
        SerieGraphiqueDTO poids = new SerieGraphiqueDTO("Poids du mois", saisonnalite, StyleSerie.REALISE);
        return graphiquePilotageService.dessinerBarres(mois, List.of(poids), UniteIndicateur.POURCENTAGE, FormatGraphe.MOITIE);
    }

    /** Mois × années ; le mois en cours porte « (au 15) » : l'année en cours y est incomplète. */
    private static TableauRapportDTO construireMoisAnnees(ComparaisonAnneesDTO comparaison, List<String> mois) {
        List<AnneeCompareeDTO> annees = comparaison.annees();
        IndicateurPilotageDTO indicateur = comparaison.indicateur();
        LocalDate jusquAu = comparaison.jusquAu();
        List<LigneRapportDTO> lignes = new ArrayList<>(MOIS_PAR_AN + 1);
        for (int rang = 0; rang < MOIS_PAR_AN; rang++) {
            List<CelluleRapportDTO> cellules = new ArrayList<>(annees.size());
            for (AnneeCompareeDTO annee : annees) {
                cellules.add(TableauxRapport.lireCellule(annee.mois().get(rang), indicateur));
            }
            String libelle = rang == jusquAu.getMonthValue() - 1 ? mois.get(rang) + " (au " + jusquAu.getDayOfMonth() + ")" : mois.get(rang);
            lignes.add(new LigneRapportDTO(libelle, cellules));
        }
        List<CelluleRapportDTO> totaux = new ArrayList<>(annees.size());
        for (AnneeCompareeDTO annee : annees) {
            totaux.add(TableauxRapport.lireCellule(annee.total(), indicateur));
        }
        lignes.add(new LigneRapportDTO("Année", totaux, true));
        return new TableauRapportDTO("Mois / Année", annees.stream().map(RapportComparerAnneesServiceImpl::libellerAnnee).toList(), lignes);
    }

    private static TableauRapportDTO construireSynthese(List<SyntheseAnneeDTO> synthese) {
        List<LigneRapportDTO> lignes = new ArrayList<>(synthese.size());
        for (SyntheseAnneeDTO annee : synthese) {
            lignes.add(
                new LigneRapportDTO(
                    libellerAnnee(annee.annee(), annee.complete()),
                    List.of(
                        lireMontant(annee.caTtc()),
                        new CelluleRapportDTO("", FormatRapport.formaterVariation(null, annee.croissanceCa(), UniteIndicateur.MONTANT, SensFavorable.HAUSSE)),
                        lireMontant(annee.margeBrute()),
                        new CelluleRapportDTO(FormatRapport.formaterValeur(annee.tauxMarge(), UniteIndicateur.POURCENTAGE), null),
                        new CelluleRapportDTO(FormatRapport.formaterValeur(annee.nbVentes(), UniteIndicateur.NOMBRE), null),
                        lireMontant(annee.panierMoyen()),
                        lireMontant(annee.remises())
                    )
                )
            );
        }
        return new TableauRapportDTO("Année", COLONNES_SYNTHESE, lignes);
    }

    private static CelluleRapportDTO lireMontant(Double montant) {
        return new CelluleRapportDTO(FormatRapport.formaterValeurComplete(montant, UniteIndicateur.MONTANT), null);
    }

    private static TableauRapportDTO lireCroise(String entete, CroiseAnalyseDTO croise, IndicateurPilotageDTO indicateur) {
        return croise == null ? null : TableauxRapport.limiter(TableauxRapport.lireCroise(entete, croise, indicateur), LIGNES_CROISEES);
    }

    private static String libellerAnnee(AnneeCompareeDTO annee) {
        return libellerAnnee(annee.annee(), annee.complete());
    }

    private static String libellerAnnee(int annee, boolean complete) {
        return complete ? String.valueOf(annee) : annee + " (en cours)";
    }
}
