package com.kobe.warehouse.service.promotion;

/**
 * Ce qu'une promotion de groupe (offre N+M, pack) pose sur chaque ligne du panier, dans l'ordre des
 * lignes reçues.
 *
 * @param remises la remise de chaque ligne, en francs ; leur somme est la remise du groupe
 * @param unites les unités de chaque ligne prises dans un groupe complet
 */
public record RemiseRepartie(int[] remises, int[] unites) {

    public long remiseTotale() {
        long total = 0;
        for (int remise : remises) {
            total += remise;
        }
        return total;
    }

    public int unitesTotales() {
        int total = 0;
        for (int u : unites) {
            total += u;
        }
        return total;
    }
}
