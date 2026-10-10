package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.ObjectifPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Objectifs mensuels ramenés à une tranche quelconque. Un montant se somme mois par mois, au prorata des jours couverts
 * (l'objectif d'octobre au 10 vaut 10/31 de l'objectif d'octobre) ; un taux ne vaut que sur un seul mois. Un mois sans objectif
 * laisse la tranche sans objectif : on ne compare pas à un objectif incomplet.
 */
public final class ObjectifsPeriode {

    public static final ObjectifsPeriode AUCUN = new ObjectifsPeriode();

    private final Map<IndicateurPilotage, Map<YearMonth, Double>> parMois = new EnumMap<>(IndicateurPilotage.class);

    public static ObjectifsPeriode ranger(List<ObjectifPilotage> objectifs) {
        ObjectifsPeriode periode = new ObjectifsPeriode();
        for (ObjectifPilotage objectif : objectifs) {
            periode.parMois.computeIfAbsent(objectif.getIndicateur(), cle -> new HashMap<>()).put(YearMonth.of(objectif.getAnnee(), objectif.getMois()), objectif.getValeur());
        }
        return periode;
    }

    public Double lireObjectif(IndicateurPilotage indicateur, PeriodeDTO tranche) {
        Map<YearMonth, Double> mois = parMois.get(indicateur);
        if (mois == null) {
            return null;
        }
        YearMonth premier = YearMonth.from(tranche.du());
        YearMonth dernier = YearMonth.from(tranche.au());
        if (!indicateur.estAdditif()) {
            return premier.equals(dernier) ? mois.get(premier) : null;
        }
        double somme = 0;
        for (YearMonth courant = premier; !courant.isAfter(dernier); courant = courant.plusMonths(1)) {
            Double objectif = mois.get(courant);
            if (objectif == null) {
                return null;
            }
            somme += objectif * compterJoursCouverts(courant, tranche) / courant.lengthOfMonth();
        }
        return somme;
    }

    /** Objectif du mois entier qui contient ce jour. */
    public Double lireObjectifDuMois(IndicateurPilotage indicateur, LocalDate jour) {
        Map<YearMonth, Double> mois = parMois.get(indicateur);
        return mois == null ? null : mois.get(YearMonth.from(jour));
    }

    private static long compterJoursCouverts(YearMonth mois, PeriodeDTO tranche) {
        LocalDate debut = tranche.du().isAfter(mois.atDay(1)) ? tranche.du() : mois.atDay(1);
        LocalDate fin = tranche.au().isBefore(mois.atEndOfMonth()) ? tranche.au() : mois.atEndOfMonth();
        return ChronoUnit.DAYS.between(debut, fin) + 1;
    }
}
