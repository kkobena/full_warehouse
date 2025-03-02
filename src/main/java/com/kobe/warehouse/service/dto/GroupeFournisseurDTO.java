package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GroupeFournisseur;
import jakarta.validation.constraints.NotNull;
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

    public GroupeFournisseurDTO() {}

    public GroupeFournisseurDTO(GroupeFournisseur groupeFournisseur) {
        id = groupeFournisseur.getId();
        libelle = groupeFournisseur.getLibelle();
        addresspostale = groupeFournisseur.getAddresspostale();
        numFaxe = groupeFournisseur.getNumFaxe();
        email = groupeFournisseur.getEmail();
        tel = groupeFournisseur.getTel();
        odre = groupeFournisseur.getOdre();
    }

    public Long getId() {
        return id;
    }

    public GroupeFournisseurDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull String getLibelle() {
        return libelle;
    }

    public GroupeFournisseurDTO setLibelle(@NotNull String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getAddresspostale() {
        return addresspostale;
    }

    public GroupeFournisseurDTO setAddresspostale(String addresspostale) {
        this.addresspostale = addresspostale;
        return this;
    }

    public String getNumFaxe() {
        return numFaxe;
    }

    public GroupeFournisseurDTO setNumFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public GroupeFournisseurDTO setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getTel() {
        return tel;
    }

    public GroupeFournisseurDTO setTel(String tel) {
        this.tel = tel;
        return this;
    }

    public Integer getOdre() {
        return odre;
    }

    public GroupeFournisseurDTO setOdre(Integer odre) {
        this.odre = odre;
        return this;
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
}
