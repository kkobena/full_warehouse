package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.service.dto.pilotage.LigneMargeDTO;

/** CA HT et marge d'un élément sur la période et la référence ; taux en fraction (0,25 pour 25 %). */
public record MargeElement(long caHt, long marge, long caHtReference, long margeReference) {
    private static final double CENT = 100.0;

    public static MargeElement lire(MesuresComparees mesures) {
        return new MargeElement(
            mesures.periode().caLignesHt(),
            mesures.periode().margeBrute(),
            mesures.reference().caLignesHt(),
            mesures.reference().margeBrute()
        );
    }

    public Double taux() {
        return caHt == 0 ? null : marge * CENT / caHt;
    }

    public Double tauxReference() {
        return caHtReference == 0 ? null : margeReference * CENT / caHtReference;
    }

    /** Sans CA de référence (élément nouveau), le taux de la période : tout l'écart est alors de l'effet volume. */
    public double tauxDeReference() {
        if (caHtReference != 0) {
            return (double) margeReference / caHtReference;
        }
        return caHt == 0 ? 0 : (double) marge / caHt;
    }

    /** Sans CA sur la période (élément disparu), le taux de référence : l'effet taux est nul. */
    public double tauxDeLaPeriode() {
        return caHt == 0 ? tauxDeReference() : (double) marge / caHt;
    }

    public double poids(long caHtTotal) {
        return caHtTotal == 0 ? 0 : (double) caHt / caHtTotal;
    }

    public double poidsReference(long caHtTotalReference) {
        return caHtTotalReference == 0 ? 0 : (double) caHtReference / caHtTotalReference;
    }

    public LigneMargeDTO versLigne(MesuresComparees element, boolean avecReference) {
        Double taux = taux();
        Double tauxReference = avecReference ? tauxReference() : null;
        return new LigneMargeDTO(
            element.membre().cle(),
            element.membre().libelle(),
            caHt,
            element.periode().coutHt(),
            marge,
            taux,
            tauxReference,
            Variations.ecart(taux, tauxReference),
            avecReference ? Math.round((caHt - caHtReference) * tauxDeReference()) : null,
            avecReference ? Math.round(caHt * (tauxDeLaPeriode() - tauxDeReference())) : null
        );
    }
}
