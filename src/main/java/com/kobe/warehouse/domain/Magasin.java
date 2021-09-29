package com.kobe.warehouse.domain;


import org.hibernate.annotations.JoinFormula;

import javax.persistence.*;
import javax.validation.constraints.*;

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

    public Storage getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(Storage pointOfSale) {
        this.pointOfSale = pointOfSale;
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

    public Magasin name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public Magasin phone(String phone) {
        this.phone = phone;
        return this;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public Magasin address(String address) {
        this.address = address;
        return this;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNote() {
        return note;
    }

    public Magasin note(String note) {
        this.note = note;
        return this;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRegistre() {
        return registre;
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

    public void setRegistre(String registre) {
        this.registre = registre;
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
