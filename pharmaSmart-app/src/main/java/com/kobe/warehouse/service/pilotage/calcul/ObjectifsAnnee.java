package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ObjectifPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Objectifs d'une année rangés par indicateur, en un passage : les 12 mois et la dernière modification. */
public final class ObjectifsAnnee {

    private static final int MOIS_PAR_AN = 12;

    private final Map<IndicateurPilotage, Double[]> valeurs = new EnumMap<>(IndicateurPilotage.class);
    private final Map<IndicateurPilotage, ObjectifPilotage> derniers = new EnumMap<>(IndicateurPilotage.class);

    public static ObjectifsAnnee ranger(List<ObjectifPilotage> objectifs) {
        ObjectifsAnnee annee = new ObjectifsAnnee();
        for (ObjectifPilotage objectif : objectifs) {
            annee.valeurs.computeIfAbsent(objectif.getIndicateur(), cle -> new Double[MOIS_PAR_AN])[objectif.getMois() - 1] = objectif.getValeur();
            annee.derniers.merge(objectif.getIndicateur(), objectif, (a, b) -> a.getModifieLe().isAfter(b.getModifieLe()) ? a : b);
        }
        return annee;
    }

    public List<Double> lireMois(IndicateurPilotage indicateur) {
        return Arrays.asList(valeurs.getOrDefault(indicateur, new Double[MOIS_PAR_AN]));
    }

    /** Objectif d'un mois (1 à 12), {@code null} s'il n'y en a pas. */
    public Double lireObjectif(IndicateurPilotage indicateur, int mois) {
        Double[] mois12 = valeurs.get(indicateur);
        return mois12 == null ? null : mois12[mois - 1];
    }

    public String lireAuteur(IndicateurPilotage indicateur) {
        ObjectifPilotage dernier = derniers.get(indicateur);
        AppUser auteur = dernier == null ? null : dernier.getModifiePar();
        return auteur == null ? null : (auteur.getFirstName() + " " + auteur.getLastName()).trim();
    }

    public LocalDateTime lireDate(IndicateurPilotage indicateur) {
        ObjectifPilotage dernier = derniers.get(indicateur);
        return dernier == null ? null : dernier.getModifieLe();
    }
}
