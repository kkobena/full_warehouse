package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.GroupeFournisseur;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;


public class FournisseurDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;

    private String libelle;
    private String addresspostale;

    private String numFaxe;

    private String addressePostal;

    private String phone;

    private String mobile;

    private String site;
    private String code;


    private Long groupeFournisseurId;

    private String groupeFournisseurLibelle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getAddresspostale() {
        return addresspostale;
    }

    public void setAddresspostale(String addresspostale) {
        this.addresspostale = addresspostale;
    }

    public String getNumFaxe() {
        return numFaxe;
    }

    public void setNumFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
    }

    public String getAddressePostal() {
        return addressePostal;
    }

    public void setAddressePostal(String addressePostal) {
        this.addressePostal = addressePostal;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getGroupeFournisseurId() {
        return groupeFournisseurId;
    }

    public void setGroupeFournisseurId(Long groupeFournisseurId) {
        this.groupeFournisseurId = groupeFournisseurId;
    }

    public String getGroupeFournisseurLibelle() {
        return groupeFournisseurLibelle;
    }

    public void setGroupeFournisseurLibelle(String groupeFournisseurLibelle) {
        this.groupeFournisseurLibelle = groupeFournisseurLibelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FournisseurDTO)) {
            return false;
        }

        return id != null && id.equals(((FournisseurDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }



    public FournisseurDTO() {
    }

    public FournisseurDTO(Fournisseur fournisseur) {
        this.id = fournisseur.getId();
        this.libelle = fournisseur.getLibelle();
        this.addresspostale = fournisseur.getAddressePostal();
        this.numFaxe = fournisseur.getNumFaxe();
        this.addressePostal = fournisseur.getAddressePostal();
        this.phone = fournisseur.getPhone();
        this.mobile = fournisseur.getMobile();
        this.site = fournisseur.getSite();
        this.code = fournisseur.getCode();
        GroupeFournisseur groupeFournisseur = fournisseur.getGroupeFournisseur();
        if (groupeFournisseur != null) {
            this.groupeFournisseurId = groupeFournisseur.getId();
            this.groupeFournisseurLibelle = groupeFournisseur.getLibelle();
        }

    }
}
