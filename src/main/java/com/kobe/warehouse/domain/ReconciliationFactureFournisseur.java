package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ReconciliationStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_facture_fournisseur")
public class ReconciliationFactureFournisseur implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "facture_reference", length = 100)
    private String factureReference;

    @Column(name = "facture_date")
    private LocalDate factureDate;

    @Column(name = "facture_montant_ht")
    private Integer factureMontantHT;

    @Column(name = "facture_tva")
    private Integer factureTVA;

    @Column(name = "bl_montant_ht")
    private Integer blMontantHT;

    @Column(name = "bl_tva")
    private Integer blTVA;

    @Column(name = "ecart_ht")
    private Integer ecartHT;

    @Column(name = "ecart_tva")
    private Integer ecartTVA;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private ReconciliationStatut statut = ReconciliationStatut.EN_ATTENTE;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avoir_fournisseur_id")
    private AvoirFournisseur avoir;

    public Integer getId() { return id; }
    public ReconciliationFactureFournisseur setId(Integer id) { this.id = id; return this; }

    public String getFactureReference() { return factureReference; }
    public ReconciliationFactureFournisseur setFactureReference(String factureReference) { this.factureReference = factureReference; return this; }

    public LocalDate getFactureDate() { return factureDate; }
    public ReconciliationFactureFournisseur setFactureDate(LocalDate factureDate) { this.factureDate = factureDate; return this; }

    public Integer getFactureMontantHT() { return factureMontantHT; }
    public ReconciliationFactureFournisseur setFactureMontantHT(Integer factureMontantHT) { this.factureMontantHT = factureMontantHT; return this; }

    public Integer getFactureTVA() { return factureTVA; }
    public ReconciliationFactureFournisseur setFactureTVA(Integer factureTVA) { this.factureTVA = factureTVA; return this; }

    public Integer getBlMontantHT() { return blMontantHT; }
    public ReconciliationFactureFournisseur setBlMontantHT(Integer blMontantHT) { this.blMontantHT = blMontantHT; return this; }

    public Integer getBlTVA() { return blTVA; }
    public ReconciliationFactureFournisseur setBlTVA(Integer blTVA) { this.blTVA = blTVA; return this; }

    public Integer getEcartHT() { return ecartHT; }
    public ReconciliationFactureFournisseur setEcartHT(Integer ecartHT) { this.ecartHT = ecartHT; return this; }

    public Integer getEcartTVA() { return ecartTVA; }
    public ReconciliationFactureFournisseur setEcartTVA(Integer ecartTVA) { this.ecartTVA = ecartTVA; return this; }

    public ReconciliationStatut getStatut() { return statut; }
    public ReconciliationFactureFournisseur setStatut(ReconciliationStatut statut) { this.statut = statut; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public ReconciliationFactureFournisseur setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public ReconciliationFactureFournisseur setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

    public AvoirFournisseur getAvoir() { return avoir; }
    public ReconciliationFactureFournisseur setAvoir(AvoirFournisseur avoir) { this.avoir = avoir; return this; }
}
