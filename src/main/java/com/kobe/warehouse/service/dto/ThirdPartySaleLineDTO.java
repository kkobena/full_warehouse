package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;

import java.time.Instant;

public class ThirdPartySaleLineDTO {
    private Long id;
    private String numBon;
    private Integer montant;
    private Long clientTiersPayantId;
    private Long customerId;
    private Long tiersPayantId;
    private String customerFullName;
    private String tiersPayantFullName;
    private Instant created;
    private Instant updated;
    private Instant effectiveUpdateDate;
    private short taux;
    private ThirdPartySaleStatut statut;
    private ThirdPartySaleStatut invoiceStatut;

    public Long getId() {
        return id;
    }

    public ThirdPartySaleLineDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNumBon() {
        return numBon;
    }

    public ThirdPartySaleLineDTO setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }

    public Integer getMontant() {
        return montant;
    }

    public ThirdPartySaleLineDTO setMontant(Integer montant) {
        this.montant = montant;
        return this;
    }

    public Long getClientTiersPayantId() {
        return clientTiersPayantId;
    }

    public ThirdPartySaleLineDTO setClientTiersPayantId(Long clientTiersPayantId) {
        this.clientTiersPayantId = clientTiersPayantId;
        return this;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public ThirdPartySaleLineDTO setCustomerId(Long customerId) {
        this.customerId = customerId;
        return this;
    }

    public Long getTiersPayantId() {
        return tiersPayantId;
    }

    public ThirdPartySaleLineDTO setTiersPayantId(Long tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
        return this;
    }

    public String getCustomerFullName() {
        return customerFullName;
    }

    public ThirdPartySaleLineDTO setCustomerFullName(String customerFullName) {
        this.customerFullName = customerFullName;
        return this;
    }

    public String getTiersPayantFullName() {
        return tiersPayantFullName;
    }

    public ThirdPartySaleLineDTO setTiersPayantFullName(String tiersPayantFullName) {
        this.tiersPayantFullName = tiersPayantFullName;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public ThirdPartySaleLineDTO setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public ThirdPartySaleLineDTO setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }

    public Instant getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public ThirdPartySaleLineDTO setEffectiveUpdateDate(Instant effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public short getTaux() {
        return taux;
    }

    public ThirdPartySaleLineDTO setTaux(short taux) {
        this.taux = taux;
        return this;
    }

    public ThirdPartySaleStatut getStatut() {
        return statut;
    }

    public ThirdPartySaleLineDTO setStatut(ThirdPartySaleStatut statut) {
        this.statut = statut;
        return this;
    }

    public ThirdPartySaleStatut getInvoiceStatut() {
        return invoiceStatut;
    }

    public ThirdPartySaleLineDTO setInvoiceStatut(ThirdPartySaleStatut invoiceStatut) {
        this.invoiceStatut = invoiceStatut;
        return this;
    }

    public ThirdPartySaleLineDTO() {
    }

    public ThirdPartySaleLineDTO(ThirdPartySaleLine thirdPartySaleLine) {
        this.id = thirdPartySaleLine.getId();
        this.numBon = thirdPartySaleLine.getNumBon();
        this.montant = thirdPartySaleLine.getMontant();
        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
        TiersPayant tiersPayant = clientTiersPayant.getTiersPayant();
        AssuredCustomer assuredCustomer = clientTiersPayant.getAssuredCustomer();
        this.clientTiersPayantId = clientTiersPayant.getId();
        this.customerId = assuredCustomer.getId();
        this.tiersPayantId = tiersPayant.getId();
        this.customerFullName = String.format("%s %s", assuredCustomer.getFirstName(), assuredCustomer.getLastName());
        this.tiersPayantFullName = tiersPayant.getFullName();
        this.created = thirdPartySaleLine.getCreated();
        this.updated = thirdPartySaleLine.getUpdated();
        this.effectiveUpdateDate = thirdPartySaleLine.getEffectiveUpdateDate();
        this.taux = thirdPartySaleLine.getTaux();
        this.statut = thirdPartySaleLine.getStatut();
        this.invoiceStatut = thirdPartySaleLine.getInvoiceStatut();
    }
}
