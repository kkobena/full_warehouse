package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AssuranceSaleId;
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
    private Integer clientTiersPayantId;
    private Integer customerId;
    private Integer tiersPayantId;
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
    private AssuranceSaleId assuranceSaleId;
    private String num;
    private short tauxVente;

    public ThirdPartySaleLineDTO() {
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public short getTauxVente() {
        return tauxVente;
    }

    public void setTauxVente(short tauxVente) {
        this.tauxVente = tauxVente;
    }

    public ThirdPartySaleLineDTO(ThirdPartySaleLine thirdPartySaleLine) {
        this.assuranceSaleId = thirdPartySaleLine.getId();
        this.id = this.assuranceSaleId.getId();
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
        this.tauxVente = thirdPartySaleLine.getTauxVente();
        this.num = clientTiersPayant.getNum();

    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder from(ThirdPartySaleLine thirdPartySaleLine) {
        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
        TiersPayant tiersPayant = clientTiersPayant.getTiersPayant();
        AssuredCustomer assuredCustomer = clientTiersPayant.getAssuredCustomer();
        AssuranceSaleId assuranceSaleId = thirdPartySaleLine.getId();
        return new Builder()
            .assuranceSaleId(assuranceSaleId)
            .id(assuranceSaleId.getId())
            .numBon(thirdPartySaleLine.getNumBon())
            .montant(thirdPartySaleLine.getMontant())
            .clientTiersPayantId(clientTiersPayant.getId())
            .customerId(assuredCustomer.getId())
            .tiersPayantId(tiersPayant.getId())
            .customerFullName(String.format("%s %s", assuredCustomer.getFirstName(), assuredCustomer.getLastName()))
            .tiersPayantFullName(tiersPayant.getFullName())
            .name(tiersPayant.getName())
            .created(thirdPartySaleLine.getCreated())
            .updated(thirdPartySaleLine.getUpdated())
            .effectiveUpdateDate(thirdPartySaleLine.getEffectiveUpdateDate())
            .taux(thirdPartySaleLine.getTaux())
            .tauxVente(thirdPartySaleLine.getTauxVente())
            .num(clientTiersPayant.getNum())
            .statut(thirdPartySaleLine.getStatut())
            .priorite(clientTiersPayant.getPriorite());
    }

    public AssuranceSaleId getAssuranceSaleId() {
        return assuranceSaleId;
    }

    public void setAssuranceSaleId(AssuranceSaleId assuranceSaleId) {
        this.assuranceSaleId = assuranceSaleId;
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

    public Integer getClientTiersPayantId() {
        return clientTiersPayantId;
    }

    public ThirdPartySaleLineDTO setClientTiersPayantId(Integer clientTiersPayantId) {
        this.clientTiersPayantId = clientTiersPayantId;
        return this;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public ThirdPartySaleLineDTO setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }

    public Integer getTiersPayantId() {
        return tiersPayantId;
    }

    public ThirdPartySaleLineDTO setTiersPayantId(Integer tiersPayantId) {
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

    public static final class Builder {

        private Long id;
        private String numBon;
        private Integer montant;
        private Integer clientTiersPayantId;
        private Integer customerId;
        private Integer tiersPayantId;
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
        private AssuranceSaleId assuranceSaleId;
        private String num;
        private short tauxVente;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder numBon(String numBon) {
            this.numBon = numBon;
            return this;
        }

        public Builder montant(Integer montant) {
            this.montant = montant;
            return this;
        }

        public Builder clientTiersPayantId(Integer clientTiersPayantId) {
            this.clientTiersPayantId = clientTiersPayantId;
            return this;
        }

        public Builder customerId(Integer customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder tiersPayantId(Integer tiersPayantId) {
            this.tiersPayantId = tiersPayantId;
            return this;
        }

        public Builder customerFullName(String customerFullName) {
            this.customerFullName = customerFullName;
            return this;
        }

        public Builder tiersPayantFullName(String tiersPayantFullName) {
            this.tiersPayantFullName = tiersPayantFullName;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder num(String num) {
            this.num = name;
            return this;
        }

        public Builder created(LocalDateTime created) {
            this.created = created;
            return this;
        }

        public Builder updated(LocalDateTime updated) {
            this.updated = updated;
            return this;
        }

        public Builder effectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
            this.effectiveUpdateDate = effectiveUpdateDate;
            return this;
        }

        public Builder taux(short taux) {
            this.taux = taux;
            return this;
        }

        public Builder tauxVente(short taux) {
            this.tauxVente = taux;
            return this;
        }

        public Builder statut(ThirdPartySaleStatut statut) {
            this.statut = statut;
            return this;
        }

        public Builder montantRemise(Integer montantRemise) {
            this.montantRemise = montantRemise;
            return this;
        }

        public Builder montantNet(Integer montantNet) {
            this.montantNet = montantNet;
            return this;
        }

        public Builder priorite(PrioriteTiersPayant priorite) {
            this.priorite = priorite;
            return this;
        }

        public Builder assuranceSaleId(AssuranceSaleId assuranceSaleId) {
            this.assuranceSaleId = assuranceSaleId;
            return this;
        }

        public ThirdPartySaleLineDTO build() {
            ThirdPartySaleLineDTO dto = new ThirdPartySaleLineDTO();
            dto.id = this.id;
            dto.numBon = this.numBon;
            dto.montant = this.montant;
            dto.clientTiersPayantId = this.clientTiersPayantId;
            dto.customerId = this.customerId;
            dto.tiersPayantId = this.tiersPayantId;
            dto.customerFullName = this.customerFullName;
            dto.tiersPayantFullName = this.tiersPayantFullName;
            dto.name = this.name;
            dto.created = this.created;
            dto.updated = this.updated;
            dto.effectiveUpdateDate = this.effectiveUpdateDate;
            dto.taux = this.taux;
            dto.statut = this.statut;
            dto.montantRemise = this.montantRemise;
            dto.montantNet = this.montantNet;
            dto.priorite = this.priorite;
            dto.assuranceSaleId = this.assuranceSaleId;
            return dto;
        }
    }
}
