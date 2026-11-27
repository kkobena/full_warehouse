package com.kobe.warehouse.service.remise.dto;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une ligne de vente du journal, avec ce qu'elle a remisé et ce que ses UG ont rapporté. Montants en
 * francs TTC, sauf ceux marqués HT ; le HT est celui du net, remise déduite.
 *
 * @param partPromotion ce que la promotion a remisé ; toute la remise d'une ligne promue enregistrée
 *     avant que la part ne soit tenue (V2.1.68)
 * @param partRemiseProduit ce que la grille a remisé : le reste de la remise
 * @param unitesPromo unités prises par la promotion (celles d'un groupe complet pour une offre N+M)
 * @param grille libellé de la remise produit de la vente, quand la ligne en porte une part
 * @param tauxRemise remise de la ligne sur son montant brut, en pourcentage
 * @param remiseUg part de la remise de la ligne qui touche ses UG, au prorata des unités
 * @param coutAchatComptabilise coût que les états comptent aujourd'hui : toute la quantité, UG
 *     comprises (défaut décrit dans docs/PLAN-COUT-ACHAT-UG.md)
 * @param coutAchatReel coût des seules unités payées au fournisseur
 * @param margeUg HT net des UG, dont le coût est nul
 */
public record LigneJournalDTO(
        LocalDate date,
        LocalDateTime heure,
        String ticket,
        NatureVente natureVente,
        String caissier,
        String vendeur,
        String client,
        String cip,
        String produit,
        String famille,
        int quantite,
        int prixUnitaire,
        int montantBrut,
        int remise,
        int partPromotion,
        int partRemiseProduit,
        OrigineRemise origine,
        Long promotionId,
        String promotionLibelle,
        int unitesPromo,
        String grille,
        double tauxRemise,
        int montantNet,
        int montantHtNet,
        int quantiteUg,
        int valeurUg,
        int remiseUg,
        int htNetUg,
        int prixAchat,
        int coutAchatComptabilise,
        int coutAchatReel,
        int coutEviteUg,
        int margeComptabilisee,
        int margeReelle,
        int margeUg) {}
