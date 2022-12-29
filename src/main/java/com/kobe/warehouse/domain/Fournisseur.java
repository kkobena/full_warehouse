package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * A Fournisseur.
 */
@Entity
@Table(name = "fournisseur")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Fournisseur implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    @Column(name = "libelle", nullable = false, unique = true)
    private String libelle;
    @Column(name = "num_faxe")
    private String numFaxe;
    @Column(name = "addresse_postal")
    private String addressePostal;
    @Column(name = "phone")
    private String phone;
    @Column(name = "mobile")
    private String mobile;
    @Column(name = "site")
    private String site;
    @NotNull
    @Size(max = 70)
    @Column(name = "code")
    private String code;
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "fournisseurs", allowSetters = true)
    private GroupeFournisseur groupeFournisseur;
    @Column(name = "identifiant_repartiteur")
    private String identifiantRepartiteur;

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

    public Fournisseur libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public Fournisseur id(Long id) {
        this.id = id;
        return this;
    }

    public String getNumFaxe() {
        return numFaxe;
    }

    public void setNumFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
    }

    public Fournisseur numFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
        return this;
    }

    public String getAddressePostal() {
        return addressePostal;
    }

    public void setAddressePostal(String addressePostal) {
        this.addressePostal = addressePostal;
    }

    public Fournisseur addressePostal(String addressePostal) {
        this.addressePostal = addressePostal;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Fournisseur phone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Fournisseur mobile(String mobile) {
        this.mobile = mobile;
        return this;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public Fournisseur site(String site) {
        this.site = site;
        return this;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Fournisseur code(String code) {
        this.code = code;
        return this;
    }

    public GroupeFournisseur getGroupeFournisseur() {
        return groupeFournisseur;
    }

    public void setGroupeFournisseur(GroupeFournisseur groupeFournisseur) {
        this.groupeFournisseur = groupeFournisseur;
    }

    public Fournisseur groupeFournisseur(GroupeFournisseur groupeFournisseur) {
        this.groupeFournisseur = groupeFournisseur;
        return this;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fournisseur)) {
            return false;
        }
        return id != null && id.equals(((Fournisseur) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }


    public String getIdentifiantRepartiteur() {
        return identifiantRepartiteur;
    }

    public void setIdentifiantRepartiteur(String identifiantRepartiteur) {
        this.identifiantRepartiteur = identifiantRepartiteur;
    }

    @Override
    public String toString() {
        return "Fournisseur{" +
            "id=" + getId() +
            ", libelle='" + getLibelle() + "'" +
            ", numFaxe='" + getNumFaxe() + "'" +
            ", addressePostal='" + getAddressePostal() + "'" +
            ", phone='" + getPhone() + "'" +
            ", mobile='" + getMobile() + "'" +
            ", site='" + getSite() + "'" +
            ", code='" + getCode() + "'" +
            "}";
    }
}
