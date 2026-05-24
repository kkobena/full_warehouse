package com.kobe.warehouse.service.reassort.dto;

import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.TypeReassort;

import java.time.LocalDateTime;
import java.util.List;

public class SuggestionReassortDto {
    private String reference;
    private Integer id;
    private List<LigneReassortDto> ligneReassorts;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String userFullName;
    private TypeReassort typeReassort;
    private StatutReassort statut;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<LigneReassortDto> getLigneReassorts() {
        return ligneReassorts;
    }

    public void setLigneReassorts(List<LigneReassortDto> ligneReassorts) {
        this.ligneReassorts = ligneReassorts;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public TypeReassort getTypeReassort() {
        return typeReassort;
    }

    public void setTypeReassort(TypeReassort typeReassort) {
        this.typeReassort = typeReassort;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public StatutReassort getStatut() {
        return statut;
    }

    public void setStatut(StatutReassort statut) {
        this.statut = statut;
    }
}
