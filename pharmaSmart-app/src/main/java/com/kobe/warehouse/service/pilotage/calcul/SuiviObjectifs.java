package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.service.dto.pilotage.ProjectionObjectifDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviMoisDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Objectif face au réalisé, et projection de fin du mois en cours. */
public final class SuiviObjectifs {

    private static final double CENT = 100.0;
    private static final DateTimeFormatter MOIS_ANNEE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private SuiviObjectifs() {}

    public static SuiviMoisDTO suivre(IndicateurPilotage indicateur, int mois, Double objectif, Double realise, boolean enCours) {
        return new SuiviMoisDTO(mois, objectif, realise, Variations.ecart(realise, objectif), atteindre(realise, objectif), tenir(indicateur, realise, objectif), enCours);
    }

    /**
     * Additif : réalisé à date × (mois N-1 entier / mois N-1 au même jour), le profil du mois N-1 ; sans vente N-1 à ce jour, au
     * rythme des jours écoulés. Un taux ne se projette pas : il vaut sa valeur à date.
     */
    public static ProjectionObjectifDTO projeter(
        IndicateurPilotage indicateur,
        Double objectif,
        MesuresPilotage realise,
        MesuresPilotage n1ADate,
        MesuresPilotage n1Reste,
        LocalDate aujourdhui
    ) {
        Double realiseADate = CalculateurIndicateurs.calculer(indicateur, realise);
        Double projection;
        String methode;
        if (realiseADate == null || !indicateur.estAdditif()) {
            projection = realiseADate;
            methode = "Un taux ne se projette pas : valeur à date.";
        } else {
            Double aDateN1 = CalculateurIndicateurs.calculer(indicateur, n1ADate);
            Double resteN1 = CalculateurIndicateurs.calculer(indicateur, n1Reste);
            String moisN1 = aujourdhui.minusYears(1).format(MOIS_ANNEE);
            if (aDateN1 != null && aDateN1 > 0 && resteN1 != null) {
                projection = realiseADate * (aDateN1 + resteN1) / aDateN1;
                methode = "D'après " + moisN1 + " : au même jour, " + Math.round(aDateN1 * CENT / (aDateN1 + resteN1)) + " % du mois était fait.";
            } else {
                projection = realiseADate * aujourdhui.lengthOfMonth() / aujourdhui.getDayOfMonth();
                methode = "Au rythme des jours écoulés : pas de ventes en " + moisN1 + " pour s'y référer.";
            }
        }
        return new ProjectionObjectifDTO(realiseADate, projection, objectif, atteindre(projection, objectif), tenir(indicateur, projection, objectif), methode);
    }

    private static Double atteindre(Double realise, Double objectif) {
        return realise == null || objectif == null || objectif == 0 ? null : realise * CENT / objectif;
    }

    /** Un indicateur à faire baisser (taux de remise) a un plafond : il est tenu tant qu'on reste dessous. */
    private static Boolean tenir(IndicateurPilotage indicateur, Double realise, Double objectif) {
        if (realise == null || objectif == null) {
            return null;
        }
        return indicateur.getSensFavorable() == SensFavorable.BAISSE ? realise <= objectif : realise >= objectif;
    }
}
