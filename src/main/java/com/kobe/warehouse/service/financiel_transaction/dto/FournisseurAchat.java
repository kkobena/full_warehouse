package com.kobe.warehouse.service.financiel_transaction.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FournisseurAchat {

    private long id;
    private String libelle;
    private AchatDTO achat;
    private List<AchatDTO> achats = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<AchatDTO> getAchats() {
        return achats;
    }

    public void setAchats(List<AchatDTO> achats) {
        this.achats = achats;
    }

    public AchatDTO getAchat() {
        return achat;
    }

    public void setAchat(AchatDTO achat) {
        this.achat = achat;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FournisseurAchat that = (FournisseurAchat) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
