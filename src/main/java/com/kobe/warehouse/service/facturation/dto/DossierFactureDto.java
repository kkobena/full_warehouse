package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import java.time.LocalDateTime;

public class DossierFactureDto {

    private Long id;
    private AssuredCustomerDTO assuredCustomer;
    private LocalDateTime createdAt;
    private String numBon;
    private int montantVente;
    private int montantBon;

    public AssuredCustomerDTO getAssuredCustomer() {
        return assuredCustomer;
    }

    public DossierFactureDto setAssuredCustomer(AssuredCustomerDTO assuredCustomer) {
        this.assuredCustomer = assuredCustomer;
        return this;
    }

    public Long getId() {
        return id;
    }

    public DossierFactureDto setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public DossierFactureDto setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public int getMontantBon() {
        return montantBon;
    }

    public DossierFactureDto setMontantBon(int montantBon) {
        this.montantBon = montantBon;
        return this;
    }

    public int getMontantVente() {
        return montantVente;
    }

    public DossierFactureDto setMontantVente(int montantVente) {
        this.montantVente = montantVente;
        return this;
    }

    public String getNumBon() {
        return numBon;
    }

    public DossierFactureDto setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }
}
