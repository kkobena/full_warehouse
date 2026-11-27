package com.kobe.warehouse.service.dto.promotion;

import java.time.LocalDate;
import java.util.List;

/**
 * Le bilan d'une promotion (phase 3 du plan) : ce qu'elle a coûté, ce qu'elle a fait vendre, et
 * l'effet sur la marge de ses articles comparé à la période précédente de même durée.
 *
 * <p>Sur les ventes clôturées, non annulées, du chiffre d'affaires (une vente dépôt est un
 * transfert), dans le magasin de l'utilisateur. Les montants sont TTC ; la marge est le montant net
 * (remises déduites) moins le coût d'achat enregistré sur chaque ligne au moment de la vente.
 *
 * @param debut premier jour de l'opération
 * @param fin dernier jour pris en compte : la fin de l'opération, ou aujourd'hui si elle court encore
 * @param debutAvant premier jour de la période de comparaison, qui finit la veille de {@code debut}
 * @param remise ce que l'opération a coûté : la remise des lignes qu'elle a posées
 * @param unitesRemisees unités remisées par l'opération (pour une offre, les seules unités offertes)
 * @param tickets ventes portant au moins une ligne de l'opération
 * @param clients clients identifiés distincts sur ces ventes ; une vente anonyme n'en compte pas
 * @param pendant les articles de l'opération, toutes ventes confondues, pendant l'opération
 * @param avant les mêmes articles sur la période de comparaison
 */
public record BilanPromotionDTO(
        Long promotionId,
        String code,
        String libelle,
        LocalDate debut,
        LocalDate fin,
        LocalDate debutAvant,
        LocalDate finAvant,
        long remise,
        long unitesRemisees,
        long tickets,
        long clients,
        Periode pendant,
        Periode avant,
        List<Article> articles) {

    /**
     * Les ventes des articles de l'opération sur une période.
     *
     * @param chiffreAffaires montant net, remises déduites
     */
    public record Periode(long unites, long chiffreAffaires, long marge, long tickets, long clients) {}

    /** Un article de l'opération : pendant, et sur la période de comparaison. */
    public record Article(
            Integer produitId,
            String libelle,
            long unites,
            long unitesAvant,
            long unitesRemisees,
            long remise,
            long chiffreAffaires,
            long marge,
            long margeAvant) {}
}
