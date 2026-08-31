package com.kobe.warehouse.service.stock.dto;

/**
 * Indicateurs de l'écran « Gestion des péremptions ».
 *
 * <p>{@code prochainesPerimes} et {@code retoursFourn} alimentent deux cartes cliquables du
 * bandeau. Elles existaient côté Angular ({@code LotPerimeValeurSum} dans
 * {@code lot-perimes.ts}) mais pas ici : les deux indicateurs s'affichaient donc VIDES, sans
 * erreur, l'API n'envoyant simplement jamais ces champs.
 *
 * <p>Les deux se calculent à des endroits différents, et c'est délibéré :
 * <ul>
 *   <li>{@code prochainesPerimes} porte sur les lots, dans le même périmètre filtré que les
 *   autres agrégats : il est produit par la requête elle-même ;</li>
 *   <li>{@code retoursFourn} compte des bons de retour, une autre racine d'agrégat, hors du
 *   périmètre de filtrage des lots : il est ajouté par le service.</li>
 * </ul>
 */
public record LotPerimeValeurSum(
    long valeurAchat,
    long valeurVente,
    int quantite,
    long count,
    /** Nombre de lots dont la péremption tombe dans les 30 prochains jours. */
    long prochainesPerimes,
    /** Nombre de bons de retour fournisseur non clôturés. */
    long retoursFourn
) {
    /** Reprend les agrégats de lots en y ajoutant le compte des retours en cours. */
    public LotPerimeValeurSum avecRetours(long retours) {
        return new LotPerimeValeurSum(valeurAchat, valeurVente, quantite, count, prochainesPerimes, retours);
    }
}
