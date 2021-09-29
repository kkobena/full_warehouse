package com.kobe.warehouse.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;


/**
 * A GroupeFournisseur.
 */
@Entity
@Table(name = "groupe_fournisseur")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupeFournisseur implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "libelle", nullable = false, unique = true)
    private String libelle;

    @Column(name = "addresspostale")
    private String addresspostale;

    @Column(name = "num_faxe")
    private String numFaxe;

    @Column(name = "email")
    private String email;

    @Column(name = "tel")
    private String tel;

    @NotNull
    @Column(name = "odre", nullable = false)
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

    public GroupeFournisseur libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public GroupeFournisseur id(Long id) {
        this.id = id;
        return this;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getAddresspostale() {
        return addresspostale;
    }

    public GroupeFournisseur addresspostale(String addresspostale) {
        this.addresspostale = addresspostale;
        return this;
    }

    public void setAddresspostale(String addresspostale) {
        this.addresspostale = addresspostale;
    }

    public String getNumFaxe() {
        return numFaxe;
    }

    public GroupeFournisseur numFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
        return this;
    }

    public void setNumFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
    }

    public String getEmail() {
        return email;
    }

    public GroupeFournisseur email(String email) {
        this.email = email;
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTel() {
        return tel;
    }

    public GroupeFournisseur tel(String tel) {
        this.tel = tel;
        return this;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }


    public Integer getOdre() {
        return odre;
    }

    public GroupeFournisseur odre(Integer odre) {
        this.odre = odre;
        return this;
    }

    public void setOdre(Integer odre) {
        this.odre = odre;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupeFournisseur)) {
            return false;
        }
        return id != null && id.equals(((GroupeFournisseur) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GroupeFournisseur{" +
            "id=" + getId() +
            ", libelle='" + getLibelle() + "'" +
            ", addresspostale='" + getAddresspostale() + "'" +
            ", numFaxe='" + getNumFaxe() + "'" +
            ", email='" + getEmail() + "'" +
            ", tel='" + getTel() + "'" +

            ", odre=" + getOdre() +
            "}";
    }
}
