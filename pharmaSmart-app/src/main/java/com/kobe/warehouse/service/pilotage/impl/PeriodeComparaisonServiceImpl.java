package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.PeriodePilotage;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

/**
 * Une période en cours se compare à date : 1er–9 octobre contre 1er–9 octobre N-1, jamais contre un mois entier. Une période
 * calée sur des mois entiers se décale de mois en mois (le 31 tombe sur le dernier jour du mois visé) ; une autre, de sa
 * longueur en jours.
 */
@Service
public class PeriodeComparaisonServiceImpl implements PeriodeComparaisonService {

    private static final int ANNEES_EN_ARRIERE_MAX = 5;

    @Override
    public ComparaisonPeriodesDTO comparer(RequetePilotageDTO requete, LocalDate aujourdhui) {
        PeriodeDTO demandee = PeriodePilotage.verifier(requete.du(), requete.au());
        boolean aDate = requete.aDate() && demandee.au().isAfter(aujourdhui) && !demandee.du().isAfter(aujourdhui);
        PeriodeDTO periode = aDate ? new PeriodeDTO(demandee.du(), aujourdhui) : demandee;
        return new ComparaisonPeriodesDTO(periode, calculerReference(requete, demandee, periode), aDate);
    }

    private PeriodeDTO calculerReference(RequetePilotageDTO requete, PeriodeDTO demandee, PeriodeDTO periode) {
        return switch (requete.comparaison()) {
            case AUCUNE, OBJECTIF -> null;
            case PERIODE_PRECEDENTE -> precedente(demandee, periode);
            case MEME_PERIODE_N_1 -> reculerAnnees(periode, 1);
            case ANNEE_N_MOINS_K -> reculerAnnees(periode, verifierAnnees(requete.anneesEnArriere()));
            case PERSONNALISEE -> PeriodePilotage.verifier(requete.referenceDu(), requete.referenceAu());
        };
    }

    private static PeriodeDTO precedente(PeriodeDTO demandee, PeriodeDTO periode) {
        if (estCaleeSurDesMois(demandee)) {
            long mois = ChronoUnit.MONTHS.between(demandee.du(), demandee.au().plusDays(1));
            return new PeriodeDTO(periode.du().minusMonths(mois), periode.au().minusMonths(mois));
        }
        long jours = demandee.nombreDeJours();
        return new PeriodeDTO(periode.du().minusDays(jours), periode.au().minusDays(jours));
    }

    private static PeriodeDTO reculerAnnees(PeriodeDTO periode, int annees) {
        return new PeriodeDTO(periode.du().minusYears(annees), periode.au().minusYears(annees));
    }

    private static boolean estCaleeSurDesMois(PeriodeDTO periode) {
        return periode.du().getDayOfMonth() == 1 && periode.au().equals(periode.au().withDayOfMonth(periode.au().lengthOfMonth()));
    }

    private static int verifierAnnees(int annees) {
        if (annees < 1 || annees > ANNEES_EN_ARRIERE_MAX) {
            throw new GenericError("La comparaison remonte de 1 à " + ANNEES_EN_ARRIERE_MAX + " ans");
        }
        return annees;
    }
}
