package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "banque")
public class Banque implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "beneficiaire")
    private String beneficiaire;

    public Banque() {}

    public String getCode() {
        return code;
    }

    public Banque setCode(String code) {
        this.code = code;
        return this;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public Banque setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAdresse() {
        return adresse;
    }

    public Banque setAdresse(String adresse) {
        this.adresse = adresse;
        return this;
    }

    public String getNom() {
        return nom;
    }

    public Banque setNom(String nom) {
        this.nom = nom;
        return this;
    }
}
