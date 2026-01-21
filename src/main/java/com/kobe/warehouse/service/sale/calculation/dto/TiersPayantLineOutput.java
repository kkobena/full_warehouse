package com.kobe.warehouse.service.sale.calculation.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TiersPayantLineOutput {

    private Integer clientTiersPayantId;
    private BigDecimal montant;
    private int finalTaux;
    private List<TvaRepartitionDto> repartitions = new ArrayList<>();

    public Integer getClientTiersPayantId() {
        return clientTiersPayantId;
    }

    public void setClientTiersPayantId(Integer clientTiersPayantId) {
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

    public List<TvaRepartitionDto> getRepartitions() {
        return repartitions;
    }

    public void setRepartitions(List<TvaRepartitionDto> repartitions) {
        this.repartitions = repartitions;
    }
}
