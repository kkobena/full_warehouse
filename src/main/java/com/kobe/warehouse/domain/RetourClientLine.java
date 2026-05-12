package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "retour_client_line")
public class RetourClientLine implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "retour_client_id", nullable = false)
    private RetourClient retourClient;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @NotNull
    @Column(name = "quantite", nullable = false)
    private int quantite;

    @NotNull
    @Column(name = "prix_unitaire", nullable = false)
    private int prixUnitaire;

    @NotNull
    @Column(name = "montant", nullable = false)
    private int montant;

    @Column(name = "original_sales_line_id")
    private Long originalSalesLineId;
    @Transient
    private int quantiteInit;

    @Transient
    private int prixAchat;

    @Transient
    private boolean stockRestitue = true;

    @Column(name = "original_sales_line_date")
    private LocalDate originalSalesLineDate;

    @Column(name = "montant_tp", nullable = false, columnDefinition = "int default 0")
    private int montantTp = 0;

    @Column(name = "emballage_intact", nullable = false, columnDefinition = "boolean default true")
    private boolean emballageIntact = true;

    @Column(name = "num_lot_lisible", nullable = false, columnDefinition = "boolean default true")
    private boolean numLotLisible = true;

    @Column(name = "date_peremption_valide", nullable = false, columnDefinition = "boolean default true")
    private boolean datePeremptionValide = true;

    public Integer getId() {
        return id;
    }

    public int getPrixAchat() {
        return prixAchat;
    }

    public RetourClientLine setPrixAchat(int prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public int getQuantiteInit() {
        return quantiteInit;
    }

    public RetourClientLine setQuantiteInit(int quantiteInit) {
        this.quantiteInit = quantiteInit;
        return this;
    }

    public RetourClientLine setId(Integer id) {
        this.id = id;
        return this;
    }

    public RetourClient getRetourClient() {
        return retourClient;
    }

    public RetourClientLine setRetourClient(RetourClient retourClient) {
        this.retourClient = retourClient;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public RetourClientLine setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public int getQuantite() {
        return quantite;
    }

    public RetourClientLine setQuantite(int quantite) {
        this.quantite = quantite;
        return this;
    }

    public int getPrixUnitaire() {
        return prixUnitaire;
    }

    public RetourClientLine setPrixUnitaire(int prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        return this;
    }

    public int getMontant() {
        return montant;
    }

    public RetourClientLine setMontant(int montant) {
        this.montant = montant;
        return this;
    }

    public Long getOriginalSalesLineId() {
        return originalSalesLineId;
    }

    public RetourClientLine setOriginalSalesLineId(Long originalSalesLineId) {
        this.originalSalesLineId = originalSalesLineId;
        return this;
    }

    public LocalDate getOriginalSalesLineDate() {
        return originalSalesLineDate;
    }

    public RetourClientLine setOriginalSalesLineDate(LocalDate originalSalesLineDate) {
        this.originalSalesLineDate = originalSalesLineDate;
        return this;
    }

    public boolean isStockRestitue() {
        return stockRestitue;
    }

    public RetourClientLine setStockRestitue(boolean stockRestitue) {
        this.stockRestitue = stockRestitue;
        return this;
    }

    public int getMontantTp() {
        return montantTp;
    }

    public RetourClientLine setMontantTp(int montantTp) {
        this.montantTp = montantTp;
        return this;
    }

    public boolean isEmballageIntact() { return emballageIntact; }
    public RetourClientLine setEmballageIntact(boolean emballageIntact) { this.emballageIntact = emballageIntact; return this; }

    public boolean isNumLotLisible() { return numLotLisible; }
    public RetourClientLine setNumLotLisible(boolean numLotLisible) { this.numLotLisible = numLotLisible; return this; }

    public boolean isDatePeremptionValide() { return datePeremptionValide; }
    public RetourClientLine setDatePeremptionValide(boolean datePeremptionValide) { this.datePeremptionValide = datePeremptionValide; return this; }
}
