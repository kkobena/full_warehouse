package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import com.kobe.warehouse.service.dto.pilotage.AnneeCompareeDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MoisRecordDTO;
import com.kobe.warehouse.service.dto.pilotage.SyntheseAnneeDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Valeurs de l'indicateur lues sur le calendrier, rapportées aux jours ouvrés si demandé. */
public record LectureAnnees(
    IndicateurPilotage indicateur,
    ModeAnnees mode,
    boolean parJourOuvre,
    CalendrierMensuel calendrier,
    int anneeEnCours,
    MesuresPilotage anneePrecedenteADate,
    int moisEnCours,
    MesuresPilotage moisPrecedentADate
) {
    private static final double CENT = 100.0;
    private static final int MOIS_PAR_AN = 12;
    private static final int MOIS_PAR_TRIMESTRE = 3;

    public AnneeCompareeDTO comparer(int annee) {
        List<CelluleAnalyseDTO> mois = IntStream.rangeClosed(1, MOIS_PAR_AN)
            .mapToObj(numero ->
                calendrier.existe(annee, numero)
                    ? Variations.cellule(indicateur, valeur(calendrier.lireFenetre(annee, numero, mode)), valeur(lireFenetreDeReference(annee, numero)))
                    : Variations.cellule(indicateur, null, null)
            )
            .toList();
        List<CelluleAnalyseDTO> trimestres = IntStream.rangeClosed(1, MOIS_PAR_AN / MOIS_PAR_TRIMESTRE)
            .mapToObj(trimestre -> {
                int premier = (trimestre - 1) * MOIS_PAR_TRIMESTRE + 1;
                int dernier = premier + MOIS_PAR_TRIMESTRE - 1;
                return calendrier.existe(annee, premier)
                    ? Variations.cellule(indicateur, valeur(calendrier.lireMois(annee, premier, dernier)), valeur(lireTrimestreDeReference(annee, premier, dernier)))
                    : Variations.cellule(indicateur, null, null);
            })
            .toList();
        return new AnneeCompareeDTO(annee, annee < anneeEnCours, mois, trimestres, Variations.cellule(indicateur, valeur(total(annee)), valeur(totalDeReference(annee))));
    }

    /** La fenêtre N-1 ; celle qui finit au mois en cours s'arrête au même jour (le mois N-1 entier remplacé par sa part à date). */
    private MesuresPilotage lireFenetreDeReference(int annee, int numero) {
        MesuresPilotage fenetre = calendrier.lireFenetre(annee - 1, numero, mode);
        if (annee != anneeEnCours || numero != moisEnCours) {
            return fenetre;
        }
        return fenetre.moins(calendrier.lireMois(annee - 1, numero, numero)).plus(moisPrecedentADate);
    }

    /** Le trimestre N-1 ; celui du mois en cours s'arrête au même jour. */
    private MesuresPilotage lireTrimestreDeReference(int annee, int premier, int dernier) {
        if (annee != anneeEnCours || moisEnCours < premier || moisEnCours > dernier) {
            return calendrier.lireMois(annee - 1, premier, dernier);
        }
        MesuresPilotage moisClos = moisEnCours > premier ? calendrier.lireMois(annee - 1, premier, moisEnCours - 1) : MesuresPilotage.AUCUNE;
        return moisClos.plus(moisPrecedentADate);
    }

    /** Poids moyen de chaque mois dans l'année (%), sur les années closes ayant des ventes. */
    public List<Double> calculerSaisonnalite(List<Integer> anneesCloses) {
        Map<Integer, Double> totaux = new LinkedHashMap<>();
        for (int annee : anneesCloses) {
            double total = brut(total(annee));
            if (total > 0) {
                totaux.put(annee, total);
            }
        }
        if (totaux.isEmpty()) {
            return List.of();
        }
        return IntStream.rangeClosed(1, MOIS_PAR_AN)
            .mapToObj(numero ->
                totaux
                    .entrySet()
                    .stream()
                    .mapToDouble(total -> brut(calendrier.lireMois(total.getKey(), numero, numero)) * CENT / total.getValue())
                    .average()
                    .orElse(0)
            )
            .toList();
    }

    public MoisRecordDTO trouverMoisMaximum(List<Integer> annees) {
        MoisRecordDTO maximum = null;
        for (int annee : annees) {
            for (int numero = 1; numero <= MOIS_PAR_AN && calendrier.existe(annee, numero); numero++) {
                Double valeur = valeur(calendrier.lireMois(annee, numero, numero));
                if (valeur != null && (maximum == null || valeur > maximum.valeur())) {
                    maximum = new MoisRecordDTO(annee, numero, valeur);
                }
            }
        }
        return maximum;
    }

    public SyntheseAnneeDTO synthetiser(int annee, boolean voitLaMarge) {
        MesuresPilotage mesures = total(annee);
        Double ca = CalculateurIndicateurs.calculer(IndicateurPilotage.CA_TTC, mesures);
        Double caReference = CalculateurIndicateurs.calculer(IndicateurPilotage.CA_TTC, totalDeReference(annee));
        return new SyntheseAnneeDTO(
            annee,
            annee < anneeEnCours,
            ca,
            voitLaMarge ? CalculateurIndicateurs.calculer(IndicateurPilotage.MARGE_BRUTE, mesures) : null,
            voitLaMarge ? CalculateurIndicateurs.calculer(IndicateurPilotage.TAUX_MARGE, mesures) : null,
            mesures.nbVentes() == 0 ? null : (double) mesures.nbVentes(),
            CalculateurIndicateurs.calculer(IndicateurPilotage.PANIER_MOYEN, mesures),
            CalculateurIndicateurs.calculer(IndicateurPilotage.REMISES, mesures),
            Variations.ecartPct(IndicateurPilotage.CA_TTC, ca, caReference)
        );
    }

    public MesuresPilotage total(int annee) {
        return calendrier.lireMois(annee, 1, MOIS_PAR_AN);
    }

    /** L'année précédente entière ; pour l'année en cours, la précédente arrêtée à la même date. */
    public MesuresPilotage totalDeReference(int annee) {
        return annee == anneeEnCours ? anneePrecedenteADate : total(annee - 1);
    }

    private Double valeur(MesuresPilotage mesures) {
        Double valeur = CalculateurIndicateurs.calculer(indicateur, mesures);
        if (!parJourOuvre || valeur == null) {
            return valeur;
        }
        return mesures.joursOuvres() == 0 ? null : valeur / mesures.joursOuvres();
    }

    public double brut(MesuresPilotage mesures) {
        Double valeur = CalculateurIndicateurs.calculer(indicateur, mesures);
        return valeur == null ? 0 : valeur;
    }
}
