package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GroupeFournisseur;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * A DTO for the {@link com.kobe.warehouse.domain.GroupeFournisseur} entity.
 */
public class GroupeFournisseurDTO implements Serializable {

    private static final long serialVersionUID = -6447988428721557444L;

    private Long id;

    @NotNull
    private String libelle;

    private String addresspostale;

    private String numFaxe;

    private String email;

    private String tel;

    private Integer odre = 100;


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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }


    public Integer getOdre() {
        return odre;
    }

    public void setOdre(Integer odre) {
        this.odre = odre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupeFournisseurDTO)) {
            return false;
        }

        return id != null && id.equals(((GroupeFournisseurDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    public GroupeFournisseurDTO() {
    }

    public GroupeFournisseurDTO(GroupeFournisseur groupeFournisseur) {
        this.id = groupeFournisseur.getId();
        this.libelle = groupeFournisseur.getLibelle();
        this.addresspostale = groupeFournisseur.getAddresspostale();
        this.numFaxe = groupeFournisseur.getNumFaxe();
        this.email = groupeFournisseur.getEmail();
        this.tel = groupeFournisseur.getTel();
        this.odre = groupeFournisseur.getOdre();
    }
}
