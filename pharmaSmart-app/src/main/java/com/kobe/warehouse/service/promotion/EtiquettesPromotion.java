package com.kobe.warehouse.service.promotion;

import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.dto.promotion.EtatPromotion;
import com.kobe.warehouse.service.dto.promotion.PromotionDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionProduitDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Les étiquettes de rayon d'une promotion (plan §5.6) : une par article, répétée {@code qty} fois.
 *
 * <p>Construites depuis le détail de la promotion, qui porte déjà le prix de référence et le prix
 * d'une unité après promotion, calculés par {@link CalculPromotion} : l'étiquette affiche donc le
 * même prix que celui du comptoir.
 */
public final class EtiquettesPromotion {

    private static final DateTimeFormatter DATE_LISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private EtiquettesPromotion() {}

    /**
     * @throws BadRequestAlertException si la promotion ne s'appliquera pas au comptoir (brouillon,
     *     suspendue, terminée, annulée) : afficher en rayon un prix que la caisse ne fera pas
     *     payer, c'est la réclamation assurée.
     */
    public static List<EtiquetteDTO> construire(PromotionDTO promotion, int qty) {
        if (promotion.etat() != EtatPromotion.EN_COURS && promotion.etat() != EtatPromotion.A_VENIR) {
            throw new BadRequestAlertException(
                    "Seule une promotion validée, en cours ou à venir, s'étiquette en rayon.",
                    "promotion",
                    "etiquettenonapplicable");
        }
        if (promotion.type() == TypePromotion.PROMO_PACK) {
            return construirePack(promotion, qty);
        }
        String mention = mentionner(promotion);
        String offre = offre(promotion.type(), promotion.quantiteMin(), promotion.valeur(), promotion.seuilMontant());
        List<EtiquetteDTO> etiquettes = new ArrayList<>();
        for (PromotionProduitDTO produit : promotion.produits()) {
            if (offre != null) {
                // Une offre N+M, ou une remise de panier, n'a pas de prix unitaire remisé : l'étiquette porte le prix
                // normal et
                // l'offre, en toutes lettres. Un prix moyen barré annoncerait ce que la caisse ne fait
                // pas payer à qui n'achète qu'une unité.
                if (produit.prixReference() == null || produit.prixReference() <= 0) {
                    continue;
                }
                EtiquetteDTO etiquette = new EtiquetteDTO()
                        .setCode(produit.codeCip())
                        .setLibelle(produit.libelle())
                        .setPrix(prix(produit.prixReference()))
                        .setMention(offre + " · " + mention)
                        .setPrint(true);
                for (int i = 0; i < qty; i++) {
                    etiquettes.add(etiquette);
                }
                continue;
            }
            // Un prix fixe au-dessus du prix de vente ne remise rien : pas d'étiquette promo.
            if (produit.prixApresPromo() == null
                    || produit.prixReference() == null
                    || produit.prixApresPromo() >= produit.prixReference()) {
                continue;
            }
            EtiquetteDTO etiquette = new EtiquetteDTO()
                    .setCode(produit.codeCip())
                    .setLibelle(produit.libelle())
                    // Le prix barré sans devise : celle du prix remisé suffit, et la ligne tient alors
                    // sur le plus petit rouleau.
                    .setPrixAvant(NumberUtil.formatToString(produit.prixReference()))
                    .setPrix(prix(produit.prixApresPromo()))
                    .setMention(mention)
                    .setPrint(true);
            for (int i = 0; i < qty; i++) {
                etiquettes.add(etiquette);
            }
        }
        return etiquettes;
    }

    /**
     * L'étiquette à coller sur l'emballage d'un pack monté par l'officine (plan §5.8) : le libellé du
     * pack, son prix et son code-barres. Au scan, le comptoir ajoute les articles du pack. Rendue en
     * réglette (libellé, prix, code en Code 39), sur les formats communs.
     */
    public static List<EtiquetteDTO> construirePack(PromotionDTO promotion, int qty) {
        if (promotion.type() != TypePromotion.PROMO_PACK || promotion.codeBarrePack() == null) {
            throw new BadRequestAlertException("Cette promotion n'est pas un pack.", "promotion", "pasunpack");
        }
        if (promotion.etat() != EtatPromotion.EN_COURS && promotion.etat() != EtatPromotion.A_VENIR) {
            throw new BadRequestAlertException(
                    "Seul un pack validé, en cours ou à venir, s'étiquette.", "promotion", "etiquettenonapplicable");
        }
        EtiquetteDTO etiquette = new EtiquetteDTO()
                .setCode(promotion.codeBarrePack())
                .setLibelle(promotion.libelle())
                .setPrix(prix(Objects.requireNonNullElse(promotion.valeur(), 0)))
                .setPrint(true);
        List<EtiquetteDTO> etiquettes = new ArrayList<>();
        for (int i = 0; i < qty; i++) {
            etiquettes.add(etiquette);
        }
        return etiquettes;
    }

    /** « Jusqu'au 31/12/2026 », complété du seuil quand il y en a un : sans lui, l'étiquette ment. */
    static String mentionner(PromotionDTO promotion) {
        String mention = "Jusqu'au " + promotion.dateFin().format(DATE_LISIBLE);
        if (promotion.quantiteMin() > 1
                && promotion.type() != TypePromotion.PROMO_OFFRE_NM
                && promotion.type() != TypePromotion.PROMO_PACK) {
            mention += " · dès " + promotion.quantiteMin() + " achetés";
        }
        return mention;
    }

    /**
     * « 2 achetés + 1 offert » : l'offre N+M dite comme en rayon. Nul pour une autre mécanique.
     * {@code quantite_min} porte N, {@code valeur} M.
     */
    public static String offre(TypePromotion type, int achetees, Integer offertes) {
        return offre(type, achetees, offertes, null);
    }

    /**
     * Comme {@link #offre(TypePromotion, int, Integer)}, et « -10 % dès 20 000 CFA d'achat » pour une
     * remise de panier : son prix unitaire dépend du reste du panier, l'étiquette ne peut que le dire.
     */
    public static String offre(TypePromotion type, int achetees, Integer valeur, Integer seuilMontant) {
        if (type == TypePromotion.PROMO_SEUIL_MONTANT && valeur != null && seuilMontant != null) {
            return "-" + valeur + " % dès " + prix(seuilMontant) + " d'achat";
        }
        Integer offertes = valeur;
        if (type != TypePromotion.PROMO_OFFRE_NM || offertes == null) {
            return null;
        }
        return achetees
                + (achetees > 1 ? " achetés + " : " acheté + ")
                + offertes
                + (offertes > 1 ? " offerts" : " offert");
    }

    private static String prix(int montant) {
        return String.format("%s CFA", NumberUtil.formatToString(montant));
    }
}
