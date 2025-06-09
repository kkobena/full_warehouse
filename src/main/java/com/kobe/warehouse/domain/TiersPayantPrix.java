package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;

@Entity
@Table(name = "sales_line_price", uniqueConstraints = { @UniqueConstraint(columnNames = { "option_prix_produit_id", "sale_line_id" }) })
public class TiersPayantPrix implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private OptionPrixProduit optionPrixProduit;

    @Column(name = "prix", nullable = false)
    private int prix;

    @Column(name = "montant", nullable = false)
    private int montant;

    @NotNull
    @ManyToOne(optional = false)
    private SalesLine saleLine;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OptionPrixProduit getOptionPrixProduit() {
        return optionPrixProduit;
    }

    public void setOptionPrixProduit(OptionPrixProduit optionPrixProduit) {
        this.optionPrixProduit = optionPrixProduit;
    }

    public int getPrix() {
        return prix;
    }

    public void setPrix(int prix) {
        this.prix = prix;
    }

    public int getMontant() {
        return montant;
    }

    public void setMontant(int montant) {
        this.montant = montant;
    }

    public SalesLine getSaleLine() {
        return saleLine;
    }

    public void setSaleLine(SalesLine saleLine) {
        this.saleLine = saleLine;
    }
}
