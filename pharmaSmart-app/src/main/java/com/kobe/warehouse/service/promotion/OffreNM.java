package com.kobe.warehouse.service.promotion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * L'offre N+M (« 2 achetés, 1 offert ») sur un panier, sans base ni contexte.
 *
 * <p>Les unités de tous les articles de la promotion se mettent en commun : la cliente peut mêler
 * deux références d'une même gamme. Elles se rangent du prix le plus élevé au plus bas, se groupent
 * par N+M, et dans chaque groupe les M moins chères sont offertes. Seuls les groupes complets
 * comptent ; les unités en surplus restent au prix de référence.
 *
 * <p>La remise se répartit ensuite au plus fort reste ({@link RepartitionRemise}) sur les unités des
 * groupes, au prorata de leur montant — pas sur la seule ligne de l'unité offerte : rendre ensuite
 * l'article payé ne doit pas laisser à la cliente l'article gratuit (plan §5.7).
 *
 * <p>Un mot à ne pas confondre : {@code SalesLine.quantityUg} porte les unités gratuites <em>du
 * fournisseur</em> (plan §7.5). L'offre N+M n'y touche pas : c'est une remise comme les autres.
 */
public final class OffreNM {

    private OffreNM() {}

    /** Une ligne du panier : son prix unitaire et sa quantité. */
    public record Ligne(int prixUnitaire, int quantite) {}

    /**
     * @param achetees N, au moins 1
     * @param offertes M, au moins 1
     * @param disponibles ce que les plafonds laissent à la promotion (nul : sans borne) ; seuls des
     *     groupes complets y tiennent
     */
    public static RemiseRepartie appliquer(int achetees, int offertes, List<Ligne> lignes, Integer disponibles) {
        int[] remises = new int[lignes.size()];
        int[] unites = new int[lignes.size()];
        if (achetees < 1 || offertes < 1) {
            return new RemiseRepartie(remises, unites);
        }
        int taille = achetees + offertes;
        // Une entrée par unité, la plus chère d'abord ; à prix égal, l'ordre des lignes.
        List<int[]> parUnite = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            Ligne ligne = lignes.get(i);
            if (ligne.prixUnitaire() <= 0) {
                continue;
            }
            for (int q = 0; q < ligne.quantite(); q++) {
                parUnite.add(new int[] {i, ligne.prixUnitaire()});
            }
        }
        parUnite.sort(Comparator.comparingInt((int[] u) -> u[1]).reversed().thenComparingInt(u -> u[0]));
        int retenues = parUnite.size() - parUnite.size() % taille;
        if (disponibles != null) {
            int borne = Math.max(0, disponibles);
            retenues = Math.min(retenues, borne - borne % taille);
        }
        long remiseTotale = 0;
        long[] poids = new long[lignes.size()];
        for (int k = 0; k < retenues; k++) {
            int[] unite = parUnite.get(k);
            unites[unite[0]]++;
            poids[unite[0]] += unite[1];
            // Les M dernières de chaque groupe, donc les moins chères, sont offertes.
            if (k % taille >= achetees) {
                remiseTotale += unite[1];
            }
        }
        return new RemiseRepartie(RepartitionRemise.auPlusFortReste(remiseTotale, poids), unites);
    }
}
