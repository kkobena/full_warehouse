package com.kobe.warehouse.service.promotion;

import java.util.List;

/**
 * La remise dès un montant d'achat (« -10 % dès 20 000 F »), sur un panier, sans base ni contexte.
 *
 * <p>Le seuil se lit sur le prix de référence des lignes de la promotion — ses articles, pas tout le
 * panier : un médicament à prix administré n'aide pas à l'atteindre et n'est jamais remisé. Atteint,
 * le taux vaut pour <em>tout</em> ce montant, pas pour l'excédent : c'est ce que la cliente comprend
 * de l'affiche.
 *
 * <p>Une promotion de panier, mais pas un objet de remise au niveau de la vente : la remise se
 * répartit au plus fort reste ({@link RepartitionRemise}) sur les lignes, au prorata de leur montant.
 * Chaque ligne garde ainsi sa base de TVA exacte, son lien à la promotion pour le rapport, et un
 * retour d'article rend la part de remise qui lui revient.
 */
public final class SeuilMontant {

    private SeuilMontant() {}

    /** Une ligne du panier : son prix unitaire et sa quantité. */
    public record Ligne(int prixUnitaire, int quantite) {}

    /**
     * @param seuil le montant à atteindre, en francs
     * @param taux le taux de remise, en pourcentage entier
     */
    public static RemiseRepartie appliquer(int seuil, int taux, List<Ligne> lignes) {
        int[] remises = new int[lignes.size()];
        int[] unites = new int[lignes.size()];
        long[] poids = new long[lignes.size()];
        long montant = 0;
        for (int i = 0; i < lignes.size(); i++) {
            Ligne ligne = lignes.get(i);
            if (ligne.prixUnitaire() > 0 && ligne.quantite() > 0) {
                poids[i] = (long) ligne.prixUnitaire() * ligne.quantite();
                montant += poids[i];
            }
        }
        if (seuil < 1 || taux < 1 || montant < seuil) {
            return new RemiseRepartie(remises, unites);
        }
        for (int i = 0; i < lignes.size(); i++) {
            if (poids[i] > 0) {
                unites[i] = lignes.get(i).quantite();
            }
        }
        // Arrondi au supérieur, comme une promotion en pourcentage sur une ligne ; une seule fois, sur
        // le total : jamais un arrondi par ligne.
        long remise = (long) Math.ceil(montant * Math.min(taux, 99) / 100.0);
        return new RemiseRepartie(RepartitionRemise.auPlusFortReste(remise, poids), unites);
    }
}
