package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import java.time.LocalDateTime;

public class ThirdPartySaleLineDTO {

    private Long id;
    private String numBon;
    private Integer montant;
    private Long clientTiersPayantId;
    private Long customerId;
    private Long tiersPayantId;
    private String customerFullName;
    private String tiersPayantFullName;
    private String name;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime effectiveUpdateDate;
    private short taux;
    private ThirdPartySaleStatut statut;
    private Integer montantRemise;
    private Integer montantNet;
    private PrioriteTiersPayant priorite;

    public ThirdPartySaleLineDTO() {}

    public ThirdPartySaleLineDTO(ThirdPartySaleLine thirdPartySaleLine) {
        this.id = thirdPartySaleLine.getId().getId();
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
        this.name = tiersPayant.getName();
        this.created = thirdPartySaleLine.getCreated();
        this.updated = thirdPartySaleLine.getUpdated();
        this.effectiveUpdateDate = thirdPartySaleLine.getEffectiveUpdateDate();
        this.taux = thirdPartySaleLine.getTaux();
        this.statut = thirdPartySaleLine.getStatut();
        this.priorite = clientTiersPayant.getPriorite();
    }

    public Integer getMontantNet() {
        return montantNet;
    }

    public ThirdPartySaleLineDTO setMontantNet(Integer montantNet) {
        this.montantNet = montantNet;
        return this;
    }

    public String getName() {
        return name;
    }

    public ThirdPartySaleLineDTO setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getMontantRemise() {
        return montantRemise;
    }

    public ThirdPartySaleLineDTO setMontantRemise(Integer montantRemise) {
        this.montantRemise = montantRemise;
        return this;
    }

    public PrioriteTiersPayant getPriorite() {
        return priorite;
    }

    public void setPriorite(PrioriteTiersPayant priorite) {
        this.priorite = priorite;
    }

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

    public LocalDateTime getCreated() {
        return created;
    }

    public ThirdPartySaleLineDTO setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public ThirdPartySaleLineDTO setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public LocalDateTime getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public ThirdPartySaleLineDTO setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
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
}
