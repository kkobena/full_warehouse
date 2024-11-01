package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import java.time.LocalDateTime;

public class FactureItemDto {

    private long saleId;
    private String numBon;
    private String saleNumber;
    private int montant;
    private int montantRemise;
    private int montantVente;
    private int montantClient;
    private LocalDateTime created;
    private LocalDateTime updated;
    private ThirdPartySaleStatut statut;
    private short taux;
    private int montantRegle;
    private AssuredCustomerDTO customer;
    private AssuredCustomerDTO ayantsDroit;

    public AssuredCustomerDTO getAyantsDroit() {
        return ayantsDroit;
    }

    public FactureItemDto setAyantsDroit(AssuredCustomerDTO ayantsDroit) {
        this.ayantsDroit = ayantsDroit;
        return this;
    }

    public int getMontantClient() {
        return montantClient;
    }

    public FactureItemDto setMontantClient(int montantClient) {
        this.montantClient = montantClient;
        return this;
    }

    public String getSaleNumber() {
        return saleNumber;
    }

    public FactureItemDto setSaleNumber(String saleNumber) {
        this.saleNumber = saleNumber;
        return this;
    }

    public long getSaleId() {
        return saleId;
    }

    public FactureItemDto setSaleId(long saleId) {
        this.saleId = saleId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public FactureItemDto setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public AssuredCustomerDTO getCustomer() {
        return customer;
    }

    public FactureItemDto setCustomer(AssuredCustomerDTO customer) {
        this.customer = customer;
        return this;
    }

    public int getMontant() {
        return montant;
    }

    public FactureItemDto setMontant(int montant) {
        this.montant = montant;
        return this;
    }

    public int getMontantRegle() {
        return montantRegle;
    }

    public FactureItemDto setMontantRegle(int montantRegle) {
        this.montantRegle = montantRegle;
        return this;
    }

    public int getMontantRemise() {
        return montantRemise;
    }

    public FactureItemDto setMontantRemise(int montantRemise) {
        this.montantRemise = montantRemise;
        return this;
    }

    public int getMontantVente() {
        return montantVente;
    }

    public FactureItemDto setMontantVente(int montantVente) {
        this.montantVente = montantVente;
        return this;
    }

    public String getNumBon() {
        return numBon;
    }

    public FactureItemDto setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }

    public ThirdPartySaleStatut getStatut() {
        return statut;
    }

    public FactureItemDto setStatut(ThirdPartySaleStatut statut) {
        this.statut = statut;
        return this;
    }

    public short getTaux() {
        return taux;
    }

    public FactureItemDto setTaux(short taux) {
        this.taux = taux;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public FactureItemDto setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }
}
