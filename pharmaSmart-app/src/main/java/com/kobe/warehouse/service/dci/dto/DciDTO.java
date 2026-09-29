package com.kobe.warehouse.service.dci.dto;

import com.kobe.warehouse.domain.enumeration.StatutDci;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.kobe.warehouse.domain.Dci;

public class DciDTO {

    private long id;
    private String code;
    private String libelle;
    private StatutDci statut;

    @JsonCreator
    public DciDTO() {
    }

    public DciDTO(Dci dci) {
        this.id = dci.getId();
        this.code = dci.getCode();
        this.libelle = dci.getLibelle();
        this.statut = dci.getStatut();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public StatutDci getStatut() {
        return statut;
    }

    public void setStatut(StatutDci statut) {
        this.statut = statut;
    }
}
