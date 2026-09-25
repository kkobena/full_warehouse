package com.kobe.warehouse.service.dto.records;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record VenteRecord(
    Integer salesAmount,
    Integer amountToBePaid,
    Integer discountAmount,
    Integer costAmount,
    Integer amountToBeTakenIntoAccount,
    Integer netAmount,
    Double montantHt,
    Integer partAssure,
    Integer partTiersPayant,
    Integer restToPay,
    Integer payrollAmount,
    Integer paidAmount,
    Integer realNetAmount,
    Integer montantttcUg,
    Long saleCount,
    String type,
    SalesStatut statut,
    String group
) {
    /**
     * Marge brute de la periode : le net vendu moins le cout d'achat des lignes.
     *
     * <p>L'accesseur retournait 0 en dur -- le tableau de bord allait donc chercher sa marge dans
     * {@code mv_marge_produit}, douze mois glissants, et la courbe de marge de l'historique des
     * ventes restait plate. Les deux termes sortent pourtant de la meme requete.
     *
     * <p>{@code @JsonProperty} est indispensable : {@code marge} n'est pas un composant du record.
     */
    @JsonProperty("marge")
    public Integer marge() {
        if (nonNull(netAmount) && nonNull(costAmount)) {
            return netAmount - costAmount;
        }
        return 0;
    }

    @JsonProperty("taxAmount")
    public Integer taxAmount() {
        if (nonNull(montantHt) && nonNull(salesAmount)) {
            return BigDecimal.valueOf(salesAmount).subtract(BigDecimal.valueOf(montantHt)).setScale(0, RoundingMode.HALF_UP).intValue();
        }
        return 0;
    }

    @JsonProperty("htAmount")
    public BigDecimal htAmount() {
        if (nonNull(montantHt)) return BigDecimal.valueOf(montantHt).setScale(0, RoundingMode.HALF_UP);
        return BigDecimal.ZERO;
    }

    @JsonProperty("panierMoyen")
    public BigDecimal panierMoyen() {
        if (nonNull(montantHt) && nonNull(saleCount) && saleCount > 0) {
            return BigDecimal.valueOf(montantHt).divide(BigDecimal.valueOf(saleCount), 0, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
