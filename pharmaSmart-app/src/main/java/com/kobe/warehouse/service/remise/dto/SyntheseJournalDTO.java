package com.kobe.warehouse.service.remise.dto;

import com.kobe.warehouse.domain.enumeration.TypePromotion;
import java.time.LocalDate;
import java.util.List;

/**
 * La synthèse du journal sur la période. Les UG sont à part : ce n'est pas une remise faite à la
 * cliente, et les additionner aux remises fausserait le total des remises accordées.
 *
 * @param remiseTotale promotions et remises produit, sans les UG
 */
public record SyntheseJournalDTO(
        LocalDate du,
        LocalDate au,
        long remiseTotale,
        Totaux promotions,
        Totaux remisesProduit,
        TotauxUg ug,
        List<ParPromotion> parPromotion,
        List<ParGrille> parGrille) {

    /**
     * @param remise remise de la nature seule : la part promotion, ou la part remise produit, d'une
     *     ligne cumulée
     */
    public record Totaux(long lignes, long tickets, long unites, long montantBrut, long remise, long montantNet) {}

    /**
     * @param margeComptabilisee marge des lignes telle que les états la comptent (coût sur toute la
     *     quantité)
     * @param margeReelle marge des mêmes lignes, UG à coût nul ; l'écart est {@code coutEvite}
     */
    public record TotauxUg(
            long lignes,
            long tickets,
            long unites,
            long valeur,
            long remise,
            long htNet,
            long coutEvite,
            long marge,
            long margeComptabilisee,
            long margeReelle) {}

    /**
     * @param unitesOffertes pour une offre N+M, les unités gratuites (M par groupe complet de N+M) ;
     *     nul pour les autres mécaniques
     */
    public record ParPromotion(
            Long promotionId,
            String libelle,
            TypePromotion type,
            long lignes,
            long tickets,
            long unitesRemisees,
            Long unitesOffertes,
            long remise,
            long montantNet) {}

    public record ParGrille(String grille, long lignes, long tickets, long remise, long montantNet) {}
}
