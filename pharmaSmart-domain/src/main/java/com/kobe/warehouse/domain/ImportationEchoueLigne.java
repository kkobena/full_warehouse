package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "importation_echouee_ligne")
public class ImportationEchoueLigne implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ImportationEchoue importationEchoue;

    @Column(name = "quantity_received")
    @Min(value = 0)
    private int quantityReceived;

    @Column(name = "ug")
    @Min(value = 0)
    private int ug;

    @Column(name = "prix_un")
    @Min(value = 0)
    private int prixUn;

    @Column(name = "prix_achat")
    @Min(value = 0)
    private int prixAchat;

    @Column(name = "produit_cip")
    private String produitCip;

    @Column(name = "produit_ean")
    private String produitEan;

    @Column(name = "code_tva")
    private Integer codeTva;

    @Column(name = "date_peremption")
    private LocalDate datePeremption;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImportationEchoue getImportationEchoue() {
        return importationEchoue;
    }

    public void setImportationEchoue(ImportationEchoue importationEchoue) {
        this.importationEchoue = importationEchoue;
    }

    @Min(value = 0)
    public int getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(@Min(value = 0) int quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    @Min(value = 0)
    public int getUg() {
        return ug;
    }

    public void setUg(@Min(value = 0) int ug) {
        this.ug = ug;
    }

    @Min(value = 0)
    public int getPrixUn() {
        return prixUn;
    }

    public void setPrixUn(@Min(value = 0) int prixUn) {
        this.prixUn = prixUn;
    }

    @Min(value = 0)
    public int getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(@Min(value = 0) int prixAchat) {
        this.prixAchat = prixAchat;
    }

    public String getProduitCip() {
        return produitCip;
    }

    public void setProduitCip(String produitCip) {
        this.produitCip = produitCip;
    }

    public String getProduitEan() {
        return produitEan;
    }

    public void setProduitEan(String produitEan) {
        this.produitEan = produitEan;
    }

    public Integer getCodeTva() {
        return codeTva;
    }

    public void setCodeTva(Integer codeTva) {
        this.codeTva = codeTva;
    }

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(LocalDate datePeremption) {
        this.datePeremption = datePeremption;
    }
}
