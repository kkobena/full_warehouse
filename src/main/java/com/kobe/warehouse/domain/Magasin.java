package com.kobe.warehouse.domain;


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
