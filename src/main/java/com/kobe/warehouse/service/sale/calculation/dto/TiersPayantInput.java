package com.kobe.warehouse.service.sale.calculation.dto;

import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;

import java.math.BigDecimal;

public class TiersPayantInput {
    private Long clientTiersPayantId;
    private Long tiersPayantId;
    private String tiersPayantFullName;
    private float taux;
    private BigDecimal plafondConso;
    private BigDecimal consoMensuelle= BigDecimal.ZERO;
    private BigDecimal plafondJournalierClient;
    private PrioriteTiersPayant priorite;

    public Long getClientTiersPayantId() {
        return clientTiersPayantId;
    }

    public void setClientTiersPayantId(Long clientTiersPayantId) {
        this.clientTiersPayantId = clientTiersPayantId;
    }

    public String getTiersPayantFullName() {
        return tiersPayantFullName;
    }

    public void setTiersPayantFullName(String tiersPayantFullName) {
        this.tiersPayantFullName = tiersPayantFullName;
    }

    public float getTaux() {
        return taux;
    }

    public void setTaux(float taux) {
        this.taux = taux;
    }

    public BigDecimal getPlafondConso() {
        return plafondConso;
    }

    public void setPlafondConso(BigDecimal plafondConso) {
        this.plafondConso = plafondConso;
    }

    public BigDecimal getConsoMensuelle() {
        return consoMensuelle;
    }

    public void setConsoMensuelle(BigDecimal consoMensuelle) {
        this.consoMensuelle = consoMensuelle;
    }

    public BigDecimal getPlafondJournalierClient() {
        return plafondJournalierClient;
    }

    public void setPlafondJournalierClient(BigDecimal plafondJournalierClient) {
        this.plafondJournalierClient = plafondJournalierClient;
    }

    public Long getTiersPayantId() {
        return tiersPayantId;
    }

    public void setTiersPayantId(Long tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
    }

    public PrioriteTiersPayant getPriorite() {
        return priorite;
    }

    public void setPriorite(PrioriteTiersPayant priorite) {
        this.priorite = priorite;
    }
}
