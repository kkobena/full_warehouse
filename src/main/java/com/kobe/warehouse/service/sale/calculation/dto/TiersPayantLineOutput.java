package com.kobe.warehouse.service.sale.calculation.dto;

import java.math.BigDecimal;

public class TiersPayantLineOutput {
    private Long clientTiersPayantId;
    private BigDecimal montant;
    private int finalTaux;

    public Long getClientTiersPayantId() {
        return clientTiersPayantId;
    }
    public void setClientTiersPayantId(Long clientTiersPayantId) {
        this.clientTiersPayantId = clientTiersPayantId;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public int getFinalTaux() {
        return finalTaux;
    }

    public void setFinalTaux(int finalTaux) {
        this.finalTaux = finalTaux;
    }


}
