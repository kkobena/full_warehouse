package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.StatutLegal;
import java.time.LocalDate;

/**
 * Une ligne de la vente, vue du retour client.
 *
 * @param montantRemboursableClient pour toute la quantité vendue ; une ligne en promotion, remise déduite
 * @param promotionId une ligne en promotion se retourne en entier ou pas du tout
 * @param pack un pack se retourne avec tous ses articles
 * @param motifRefus pourquoi une ligne en promotion ne se retourne pas (vente à crédit, cumul) ; nul sinon
 */
public record SaleLineForRetourDTO(
        Long salesLineId,
        LocalDate salesLineDate,
        String produitLibelle,
        String codeCip,
        int quantitySold,
        int netUnitPrice,
        StatutLegal statutLegal,
        boolean retourInterdit,
        boolean thermosensible,
        int montantRemboursableClient,
        int montantTp,
        Long promotionId,
        String promotionLibelle,
        boolean pack,
        String motifRefus) {}
