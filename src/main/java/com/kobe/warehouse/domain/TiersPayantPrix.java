package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;

@Entity
@Table(name = "sales_line_price", uniqueConstraints = {@UniqueConstraint(columnNames = {"client_tiers_payant_id", "sale_line_id"})})
public class TiersPayantPrix implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "montant", nullable = false)
    private int montant;
    @Column(name = "prix", nullable = false)
    private int prix;
    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_line_id", referencedColumnName = "id")
    private SalesLine saleLine;
    @ManyToOne(optional = false)
    @JoinColumn(name = "client_tiers_payant_id", referencedColumnName = "id")
    private ClientTiersPayant clientTiersPayant;

    public ClientTiersPayant getClientTiersPayant() {
        return clientTiersPayant;
    }

    public void setClientTiersPayant(ClientTiersPayant clientTiersPayant) {
        this.clientTiersPayant = clientTiersPayant;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
