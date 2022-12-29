package com.kobe.warehouse.domain;


import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.JoinFormula;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import java.io.Serializable;

/**
 * A Magasin.
 */
@Entity
@Table(name = "magasin")
public class Magasin implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @NotNull
    @Column(name = "phone", nullable = false)
    private String phone;
    @Column(name = "address")
    private String address;
    @Column(name = "note")
    private String note;
    @Column(name = "registre")
    private String registre;
    @Column(name = "compte_contribuable")
    private String compteContribuable;
    @Column(name = "num_comptable")
    private String numComptable;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula("(SELECT s.id FROM storage s WHERE s.storage_type=0 AND s.magasin_id=id LIMIT 1)")
    private Storage primaryStorage;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula("(SELECT s.id FROM storage s WHERE s.storage_type=2 AND s.magasin_id=id LIMIT 1)")
    private Storage pointOfSale;
    @Column(name = "welcome_message")
    private String welcomeMessage;

    public Storage getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(Storage pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public Magasin setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
        return this;
    }

    public Storage getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(Storage primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Magasin name(String name) {
        this.name = name;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Magasin phone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Magasin address(String address) {
        this.address = address;
        return this;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Magasin note(String note) {
        this.note = note;
        return this;
    }

    public String getRegistre() {
        return registre;
    }

    public void setRegistre(String registre) {
        this.registre = registre;
    }

    public Magasin registre(String registre) {
        this.registre = registre;
        return this;
    }

    public String getCompteContribuable() {
        return compteContribuable;
    }

    public void setCompteContribuable(String compteContribuable) {
        this.compteContribuable = compteContribuable;
    }

    public String getNumComptable() {
        return numComptable;
    }

    public void setNumComptable(String numComptable) {
        this.numComptable = numComptable;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Magasin)) {
            return false;
        }
        return id != null && id.equals(((Magasin) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Magasin{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", phone='" + getPhone() + "'" +
            ", address='" + getAddress() + "'" +
            ", note='" + getNote() + "'" +
            ", registre='" + getRegistre() + "'" +
            "}";
    }
}
