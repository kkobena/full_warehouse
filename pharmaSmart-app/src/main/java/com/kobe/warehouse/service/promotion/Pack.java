package com.kobe.warehouse.service.promotion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Le pack sur un panier, sans base ni contexte (plan §5.8) : plusieurs articles différents vendus
 * ensemble à un prix.
 *
 * <p>Le panier contient {@code k} packs complets, {@code k} étant le plus petit nombre de fois que
 * chaque composant y figure à sa quantité. Un pack incomplet ne remise rien ; les unités en surplus
 * restent au prix de référence. La remise vaut le prix de référence des unités prises, moins
 * {@code k} fois le prix du pack ; elle se répartit au plus fort reste ({@link RepartitionRemise}) sur
 * les lignes, au prorata du montant de leurs unités prises — pas de {@code Math.ceil} ligne par
 * ligne, qui remiserait plus que le pack.
 */
public final class Pack {

    private Pack() {}

    /** Une ligne du panier : son article, son prix unitaire et sa quantité. */
    public record Ligne(Integer produitId, int prixUnitaire, int quantite) {}

    /**
     * @param composants la quantité de chaque article dans un pack
     * @param prixPack le prix d'un pack complet
     * @param disponibles ce que les plafonds laissent à la promotion, en unités (nul : sans borne) ;
     *     seuls des packs complets y tiennent
     */
    public static RemiseRepartie appliquer(
            Map<Integer, Integer> composants, int prixPack, List<Ligne> lignes, Integer disponibles) {
        int[] remises = new int[lignes.size()];
        int[] unites = new int[lignes.size()];
        if (composants.isEmpty() || prixPack < 1) {
            return new RemiseRepartie(remises, unites);
        }
        int taille = 0;
        int packs = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> composant : composants.entrySet()) {
            int parPack = Math.max(1, composant.getValue());
            taille += parPack;
            int presentes = lignes.stream()
                    .filter(l -> Objects.equals(l.produitId(), composant.getKey()) && l.prixUnitaire() > 0)
                    .mapToInt(Ligne::quantite)
                    .sum();
            packs = Math.min(packs, presentes / parPack);
        }
        if (disponibles != null) {
            packs = Math.min(packs, Math.max(0, disponibles) / taille);
        }
        if (packs <= 0) {
            return new RemiseRepartie(remises, unites);
        }
        // Les unités de chaque composant se prennent dans l'ordre des lignes.
        long reference = 0;
        long[] poids = new long[lignes.size()];
        for (Map.Entry<Integer, Integer> composant : composants.entrySet()) {
            int aPrendre = packs * Math.max(1, composant.getValue());
            for (int i = 0; i < lignes.size() && aPrendre > 0; i++) {
                Ligne ligne = lignes.get(i);
                if (!Objects.equals(ligne.produitId(), composant.getKey()) || ligne.prixUnitaire() <= 0) {
                    continue;
                }
                int prises = Math.min(aPrendre, ligne.quantite());
                unites[i] += prises;
                poids[i] += (long) prises * ligne.prixUnitaire();
                reference += (long) prises * ligne.prixUnitaire();
                aPrendre -= prises;
            }
        }
        long remise = Math.max(0, reference - (long) packs * prixPack);
        if (remise == 0) {
            return new RemiseRepartie(remises, new int[lignes.size()]);
        }
        return new RemiseRepartie(RepartitionRemise.auPlusFortReste(remise, poids), unites);
    }
}
