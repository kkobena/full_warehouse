package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ReconciliationFactureFournisseur;
import com.kobe.warehouse.domain.enumeration.ReconciliationStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReconciliationFactureDTO {

    private Integer id;
    private String factureReference;
    private LocalDate factureDate;
    private Integer factureMontantHT;
    private Integer factureTVA;
    private Integer blMontantHT;
    private Integer blTVA;
    private Integer ecartHT;
    private Integer ecartTVA;
    private ReconciliationStatut statut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReconciliationFactureDTO() {}

    public ReconciliationFactureDTO(ReconciliationFactureFournisseur r) {
        id = r.getId();
        factureReference = r.getFactureReference();
        factureDate = r.getFactureDate();
        factureMontantHT = r.getFactureMontantHT();
        factureTVA = r.getFactureTVA();
        blMontantHT = r.getBlMontantHT();
        blTVA = r.getBlTVA();
        ecartHT = r.getEcartHT();
        ecartTVA = r.getEcartTVA();
        statut = r.getStatut();
        createdAt = r.getCreatedAt();
        updatedAt = r.getUpdatedAt();
    }

    public Integer getId() { return id; }
    public ReconciliationFactureDTO setId(Integer id) { this.id = id; return this; }

    public String getFactureReference() { return factureReference; }
    public ReconciliationFactureDTO setFactureReference(String factureReference) { this.factureReference = factureReference; return this; }

    public LocalDate getFactureDate() { return factureDate; }
    public ReconciliationFactureDTO setFactureDate(LocalDate factureDate) { this.factureDate = factureDate; return this; }

    public Integer getFactureMontantHT() { return factureMontantHT; }
    public ReconciliationFactureDTO setFactureMontantHT(Integer factureMontantHT) { this.factureMontantHT = factureMontantHT; return this; }

    public Integer getFactureTVA() { return factureTVA; }
    public ReconciliationFactureDTO setFactureTVA(Integer factureTVA) { this.factureTVA = factureTVA; return this; }

    public Integer getBlMontantHT() { return blMontantHT; }
    public ReconciliationFactureDTO setBlMontantHT(Integer blMontantHT) { this.blMontantHT = blMontantHT; return this; }

    public Integer getBlTVA() { return blTVA; }
    public ReconciliationFactureDTO setBlTVA(Integer blTVA) { this.blTVA = blTVA; return this; }

    public Integer getEcartHT() { return ecartHT; }
    public ReconciliationFactureDTO setEcartHT(Integer ecartHT) { this.ecartHT = ecartHT; return this; }

    public Integer getEcartTVA() { return ecartTVA; }
    public ReconciliationFactureDTO setEcartTVA(Integer ecartTVA) { this.ecartTVA = ecartTVA; return this; }

    public ReconciliationStatut getStatut() { return statut; }
    public ReconciliationFactureDTO setStatut(ReconciliationStatut statut) { this.statut = statut; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public ReconciliationFactureDTO setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public ReconciliationFactureDTO setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
}
