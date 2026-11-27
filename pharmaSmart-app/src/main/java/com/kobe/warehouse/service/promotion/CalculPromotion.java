package com.kobe.warehouse.service.promotion;

import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.PromotionProduit;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import java.util.Objects;

/**
 * L'arithmétique d'une promotion sur une ligne, sans base ni contexte : ce qui se teste ici n'a
 * besoin de rien d'autre.
 *
 * <p>Les montants sont des entiers de francs. Le pourcentage est arrondi au supérieur, comme la
 * remise de grille ({@code processProductDiscount}) ; l'arrondi de caisse au multiple de 5 vient
 * après, sur le total de la vente.
 */
public final class CalculPromotion {

    private CalculPromotion() {}

    /**
     * Remise totale de la ligne. Zéro si la quantité n'atteint pas le seuil ; au-delà du plafond par
     * vente, les unités restent au prix de référence. Jamais plus que le montant des unités remisées.
     */
    public static int remiseLigne(Promotion promotion, PromotionProduit article, int prixUnitaire, int quantite) {
        return remiseLigne(promotion, article, prixUnitaire, quantite, null);
    }

    /**
     * Comme {@link #remiseLigne(Promotion, PromotionProduit, int, int)}, les unités remisées bornées
     * en plus par {@code disponibles} : ce que les plafonds laissent encore à cette ligne (nul : sans
     * borne).
     */
    public static int remiseLigne(
            Promotion promotion, PromotionProduit article, int prixUnitaire, int quantite, Integer disponibles) {
        int unites = unitesRemisees(promotion, quantite, disponibles);
        if (prixUnitaire <= 0 || unites == 0) {
            return 0;
        }
        long montantRemisable = (long) unites * prixUnitaire;
        long remise = switch (promotion.getType()) {
            case PROMO_POURCENTAGE -> (long) Math.ceil(montantRemisable * valeurOuZero(promotion) / 100.0);
            case PROMO_MONTANT -> (long) unites * Math.min(valeurOuZero(promotion), prixUnitaire);
            case PROMO_PRIX_FIXE -> {
                Integer prixPromo = article == null ? null : article.getPrixPromo();
                yield prixPromo == null || prixPromo >= prixUnitaire ? 0 : (long) unites * (prixUnitaire - prixPromo);
            }
            // Une ligne seule : ses propres groupes. Le moteur, lui, groupe tout le panier (OffreNM).
            case PROMO_OFFRE_NM -> (long) (unites / taille(promotion)) * valeurOuZero(promotion) * prixUnitaire;
            // Un pack réunit des articles différents : une ligne seule n'en fait jamais un (Pack).
            case PROMO_PACK -> 0L;
            // Une remise de panier : c'est le montant de toutes les lignes qui compte (SeuilMontant).
            case PROMO_SEUIL_MONTANT -> 0L;
        };
        return (int) Math.max(0, Math.min(remise, montantRemisable));
    }

    /**
     * Unités de la ligne que la promotion remise : zéro sous le seuil, puis la quantité bornée par le
     * plafond par vente et par {@code disponibles}. Le seuil se lit sur la quantité achetée, pas sur
     * les unités restantes : la cliente qui en prend trois a droit au prix des trois, dans la limite
     * de ce qu'il reste.
     */
    public static int unitesRemisees(Promotion promotion, int quantite, Integer disponibles) {
        if (quantite < seuil(promotion)) {
            return 0;
        }
        int unites = quantite;
        if (promotion.getQuantiteMaxParVente() != null) {
            unites = Math.min(unites, promotion.getQuantiteMaxParVente());
        }
        if (disponibles != null) {
            unites = Math.min(unites, disponibles);
        }
        if (promotion.getType() == TypePromotion.PROMO_OFFRE_NM) {
            // Seuls des groupes complets se remisent.
            unites -= Math.max(0, unites) % taille(promotion);
        }
        return Math.max(0, unites);
    }

    /**
     * La quantité qui déclenche la promotion : {@code quantite_min}, ou N+M pour une offre, dont
     * {@code quantite_min} porte N (les unités achetées) et {@code valeur} M (les offertes).
     */
    public static int seuil(Promotion promotion) {
        return switch (promotion.getType()) {
            case PROMO_OFFRE_NM, PROMO_PACK -> taille(promotion);
            default -> Math.max(1, promotion.getQuantiteMin());
        };
    }

    /** Les unités d'un groupe : N+M pour une offre, la somme des quantités de ses articles pour un pack. */
    public static int taille(Promotion promotion) {
        if (promotion.getType() == TypePromotion.PROMO_PACK) {
            return Math.max(
                    1,
                    promotion.getProduits().stream()
                            .mapToInt(PromotionProduit::getQuantite)
                            .sum());
        }
        return Math.max(1, promotion.getQuantiteMin()) + Math.max(1, valeurOuZero(promotion));
    }

    /**
     * Une mécanique qui se calcule ligne par ligne (pourcentage, montant, prix fixe) ; les autres
     * groupent plusieurs lignes et répartissent leur remise sur les prix de référence.
     */
    public static boolean estDeLigne(Promotion promotion) {
        return switch (promotion.getType()) {
            case PROMO_POURCENTAGE, PROMO_MONTANT, PROMO_PRIX_FIXE -> true;
            case PROMO_OFFRE_NM, PROMO_PACK, PROMO_SEUIL_MONTANT -> false;
        };
    }

    /**
     * La part d'une promotion cumulable sur une ligne déjà remisée de {@code remiseCaissier} : elle
     * s'applique au prix unitaire déjà remisé, puis se borne pour que la ligne reste strictement
     * au-dessus du prix d'achat de ses unités — le plancher de marge porte sur la remise cumulée, et
     * c'est la promotion qui cède, jamais la remise du caissier.
     *
     * <p>Une promotion enregistrée à perte en connaissance de cause (elle est signalée, pas refusée) a
     * déjà fixé un prix sous le prix d'achat : le plancher est alors ce prix-là. Le cumul ne descend
     * pas plus bas que ce que l'officine a accepté pour la promotion seule.
     *
     * @param prixAchat le prix d'achat d'une unité (fournisseur principal, à défaut prix de revient)
     */
    public static int remiseCumulee(
            Promotion promotion,
            PromotionProduit article,
            int prixUnitaire,
            int quantite,
            int remiseCaissier,
            int prixAchat,
            Integer disponibles) {
        if (quantite <= 0 || prixUnitaire <= 0) {
            return 0;
        }
        long montant = (long) prixUnitaire * quantite;
        long reste = montant - Math.max(0, remiseCaissier);
        if (reste <= 0) {
            return 0;
        }
        // Le prix unitaire après la remise du caissier, au franc inférieur : la promotion ne remise
        // jamais plus que ce qui reste à payer.
        int prixRemise = (int) (reste / quantite);
        int remise = remiseLigne(promotion, article, prixRemise, quantite, disponibles);
        long seule = remiseLigne(promotion, article, prixUnitaire, quantite, disponibles);
        long plancher = Math.min((long) quantite * (Math.max(0, prixAchat) + 1), montant - seule);
        long marge = reste - plancher;
        return (int) Math.max(0, Math.min(remise, marge));
    }

    /** Somme des prix de référence d'un pack : chaque article à sa quantité. */
    public static long prixReferencePack(Promotion promotion) {
        return promotion.getProduits().stream()
                .mapToLong(pp -> (long) pp.getQuantite()
                        * Objects.requireNonNullElse(pp.getProduit().getRegularUnitPrice(), 0))
                .sum();
    }

    /**
     * Prix d'une unité en promotion : ce que l'écran affiche et ce que garde le plancher de marge.
     * Pour une offre N+M, le prix moyen d'une unité dans un groupe du même article, la remise arrondie
     * au supérieur : le prix le plus bas qu'elle puisse produire, celui que le plancher doit garder.
     */
    public static int prixUnitaireApres(Promotion promotion, PromotionProduit article, int prixUnitaire) {
        if (promotion.getType() == TypePromotion.PROMO_PACK) {
            // La part de remise du pack qui revient à une unité de cet article, au prorata de son prix.
            long reference = prixReferencePack(promotion);
            long remisePack = Math.max(0, reference - valeurOuZero(promotion));
            return reference == 0 ? prixUnitaire : (int) (prixUnitaire - prixUnitaire * remisePack / reference);
        }
        if (promotion.getType() == TypePromotion.PROMO_OFFRE_NM) {
            long remise =
                    (long) Math.ceil((double) prixUnitaire * Math.max(1, valeurOuZero(promotion)) / taille(promotion));
            return (int) (prixUnitaire - remise);
        }
        // Une remise de panier, une fois le seuil atteint, remise chaque unité de son taux.
        TypePromotion type = promotion.getType() == TypePromotion.PROMO_SEUIL_MONTANT
                ? TypePromotion.PROMO_POURCENTAGE
                : promotion.getType();
        Promotion unitaire =
                new Promotion().setType(type).setValeur(promotion.getValeur()).setQuantiteMin(1);
        return prixUnitaire - remiseLigne(unitaire, article, prixUnitaire, 1);
    }

    private static int valeurOuZero(Promotion promotion) {
        return promotion.getValeur() == null ? 0 : promotion.getValeur();
    }
}
