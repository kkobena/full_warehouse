package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;

/**
 * Mesures d'un élément (ou d'un couple d'éléments, en croisé) sur la période et sur sa référence ; un élément absent d'un côté
 * y a des mesures nulles.
 */
public record MesuresComparees(MembreAnalyseDTO membre, MembreAnalyseDTO membre2, MesuresPilotage periode, MesuresPilotage reference) {
    public static final MesuresComparees AUCUNES = new MesuresComparees(null, null, MesuresPilotage.AUCUNE, MesuresPilotage.AUCUNE);

    /** Garde les libellés de l'élément déjà connu : ceux de la période, lue avant la référence. */
    public MesuresComparees plus(MesuresComparees autre) {
        return new MesuresComparees(
            membre == null ? autre.membre : membre,
            membre2 == null ? autre.membre2 : membre2,
            periode.plus(autre.periode),
            reference.plus(autre.reference)
        );
    }

    /** Sur un indicateur additif : valeur, référence et écart, arrondis au franc. */
    public ContributionDTO versContribution(AxeAnalyse axe, IndicateurPilotage indicateur) {
        long valeur = arrondir(CalculateurIndicateurs.calculer(indicateur, periode));
        long valeurReference = arrondir(CalculateurIndicateurs.calculer(indicateur, reference));
        return new ContributionDTO(axe, axe.getLibelle(), membre.cle(), membre.libelle(), valeur, valeurReference, valeur - valeurReference);
    }

    private static long arrondir(Double valeur) {
        return valeur == null ? 0 : Math.round(valeur);
    }
}
