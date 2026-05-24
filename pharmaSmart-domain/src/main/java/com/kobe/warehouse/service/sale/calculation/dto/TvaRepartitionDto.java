package com.kobe.warehouse.service.sale.calculation.dto;

import com.kobe.warehouse.domain.RepartitionTiersPayantParTva;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO for TVA (VAT) repartition with BigDecimal precision for accurate calculations.
 * This DTO is used during calculation phase. Convert to {@link RepartitionTiersPayantParTva} for persistence.
 */
public class TvaRepartitionDto {

    private BigDecimal montantTtc = BigDecimal.ZERO;
    private BigDecimal montantTva = BigDecimal.ZERO;
    private BigDecimal montantNet = BigDecimal.ZERO;
    private BigDecimal montantHt = BigDecimal.ZERO;
    private int tva;

    public TvaRepartitionDto() {
    }

    public TvaRepartitionDto(int tva) {
        this.tva = tva;
    }

    /**
     * Converts this DTO to domain record for persistence.
     *
     * @return RepartitionTiersPayantParTva record
     */
    public RepartitionTiersPayantParTva toDomainRecord() {
        return new RepartitionTiersPayantParTva(
            montantTtc.setScale(2, RoundingMode.HALF_UP).doubleValue(),
            montantTva.setScale(2, RoundingMode.HALF_UP).doubleValue(),
            montantNet.setScale(2, RoundingMode.HALF_UP).doubleValue(),
            montantHt.setScale(2, RoundingMode.HALF_UP).doubleValue(),
            tva
        );
    }

    public BigDecimal getMontantTtc() {
        return montantTtc;
    }

    public void setMontantTtc(BigDecimal montantTtc) {
        this.montantTtc = montantTtc;
    }

    public BigDecimal getMontantTva() {
        return montantTva;
    }

    public void setMontantTva(BigDecimal montantTva) {
        this.montantTva = montantTva;
    }

    public BigDecimal getMontantNet() {
        return montantNet;
    }

    public void setMontantNet(BigDecimal montantNet) {
        this.montantNet = montantNet;
    }

    public BigDecimal getMontantHt() {
        return montantHt;
    }

    public void setMontantHt(BigDecimal montantHt) {
        this.montantHt = montantHt;
    }

    public int getTva() {
        return tva;
    }

    public void setTva(int tva) {
        this.tva = tva;
    }

    /**
     * Adds amounts from another TVA repartition to this one.
     *
     * @param other the other repartition to add
     */
    public void add(TvaRepartitionDto other) {
        this.montantTtc = this.montantTtc.add(other.montantTtc);
        this.montantTva = this.montantTva.add(other.montantTva);
        this.montantNet = this.montantNet.add(other.montantNet);
        this.montantHt = this.montantHt.add(other.montantHt);
    }
}
