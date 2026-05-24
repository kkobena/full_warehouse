package com.kobe.warehouse.service.reglement.dto;

import jakarta.validation.constraints.NotNull;

public class BanqueInfoDTO {

    @NotNull
    private String nom;

    private String adresse;
    private String code;
    private String beneficiaire;

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public BanqueInfoDTO setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
        return this;
    }

    public String getCode() {
        return code;
    }

    public BanqueInfoDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public String getAdresse() {
        return adresse;
    }

    public BanqueInfoDTO setAdresse(String adresse) {
        this.adresse = adresse;
        return this;
    }

    public String getNom() {
        return nom;
    }

    public BanqueInfoDTO setNom(String nom) {
        this.nom = nom;
        return this;
    }
}
