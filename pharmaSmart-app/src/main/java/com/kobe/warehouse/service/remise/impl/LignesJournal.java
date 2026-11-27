package com.kobe.warehouse.service.remise.impl;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.service.remise.dto.LigneJournalDTO;
import com.kobe.warehouse.service.remise.dto.OrigineRemise;
import com.kobe.warehouse.service.remise.impl.JournalRemisesRequetes.Colonne;
import com.kobe.warehouse.service.sale.calculation.SaleAmountCalculator;
import jakarta.persistence.Tuple;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Une ligne lue par {@link JournalRemisesRequetes}, par le nom de ses colonnes, et ce qui s'en
 * déduit. La part de la promotion vient de la requête, qui la calcule pour filtrer
 * ({@link JournalRemisesSpecifications#calculerPartPromotion}). Les règles de calcul sont
 * ici seulement, pour que l'écran, la synthèse et les trois formats d'export disent la même chose.
 */
final class LignesJournal {

    private LignesJournal() {}

    static LigneJournalDTO lireLigne(Tuple l) {
        int quantite = lireEntier(l, Colonne.QUANTITE);
        int prix = lireEntier(l, Colonne.PRIX_UNITAIRE);
        int brut = lireEntier(l, Colonne.MONTANT_BRUT);
        int remise = lireEntier(l, Colonne.REMISE);
        int partPromotion = lireEntier(l, Colonne.PART_PROMOTION);
        Long promotionId = l.get(Colonne.PROMOTION_ID, Long.class);
        int quantiteUg = lireEntier(l, Colonne.QUANTITE_UG);
        int prixAchat = lireEntier(l, Colonne.PRIX_ACHAT);
        Integer tva = l.get(Colonne.TAUX_TVA, Integer.class);

        int partRemiseProduit = remise - partPromotion;
        int net = brut - remise;
        int htNet = SaleAmountCalculator.montantHt(net, tva);

        int valeurUg = quantiteUg * prix;
        int remiseUg = quantite == 0 ? 0 : (int) Math.round((double) remise * quantiteUg / quantite);
        int htNetUg = SaleAmountCalculator.montantHt(valeurUg - remiseUg, tva);
        int coutComptabilise = quantite * prixAchat;
        int coutReel = Math.max(0, quantite - quantiteUg) * prixAchat;

        return new LigneJournalDTO(
                l.get(Colonne.DATE, LocalDate.class),
                l.get(Colonne.HEURE, LocalDateTime.class),
                l.get(Colonne.TICKET, String.class),
                l.get(Colonne.NATURE_VENTE, NatureVente.class),
                nommer(l, Colonne.CAISSIER_PRENOM, Colonne.CAISSIER_NOM),
                nommer(l, Colonne.VENDEUR_PRENOM, Colonne.VENDEUR_NOM),
                nommer(l, Colonne.CLIENT_PRENOM, Colonne.CLIENT_NOM),
                l.get(Colonne.CIP, String.class),
                l.get(Colonne.PRODUIT, String.class),
                l.get(Colonne.FAMILLE, String.class),
                quantite,
                prix,
                brut,
                remise,
                partPromotion,
                partRemiseProduit,
                determinerOrigine(promotionId, partRemiseProduit, remise),
                promotionId,
                l.get(Colonne.PROMOTION_LIBELLE, String.class),
                lireEntier(l, Colonne.UNITES_PROMO),
                partRemiseProduit != 0 ? l.get(Colonne.GRILLE, String.class) : null,
                brut == 0 ? 0 : Math.round(remise * 10_000.0 / brut) / 100.0,
                net,
                htNet,
                quantiteUg,
                valeurUg,
                remiseUg,
                htNetUg,
                prixAchat,
                coutComptabilise,
                coutReel,
                quantiteUg * prixAchat,
                htNet - coutComptabilise,
                htNet - coutReel,
                htNetUg);
    }

    static OrigineRemise determinerOrigine(Long promotionId, int partRemiseProduit, int remise) {
        if (promotionId != null) {
            return partRemiseProduit != 0 ? OrigineRemise.CUMUL : OrigineRemise.PROMOTION;
        }
        return remise != 0 ? OrigineRemise.REMISE_PRODUIT : null;
    }

    private static int lireEntier(Tuple ligne, String colonne) {
        Number valeur = ligne.get(colonne, Number.class);
        return valeur == null ? 0 : valeur.intValue();
    }

    /** Prénom et nom, l'un ou l'autre pouvant manquer ; nul si les deux manquent. */
    private static String nommer(Tuple ligne, String prenom, String nom) {
        String complet = String.join(
                        " ",
                        Objects.requireNonNullElse(ligne.get(prenom, String.class), ""),
                        Objects.requireNonNullElse(ligne.get(nom, String.class), ""))
                .strip();
        return complet.isEmpty() ? null : complet;
    }
}
