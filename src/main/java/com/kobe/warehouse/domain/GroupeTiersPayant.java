package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "groupe_tiers_payant", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})

})
public class GroupeTiersPayant implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @Column(name = "name", nullable = false, length = 100)
    @NotNull
    private String name;
    @Column(name = "adresse", length = 200)
    private String adresse;
    @Column(name = "telephone", length = 15)
    private String telephone;
    @Column(name = "telephone_fixe", length = 15)
    private String telephoneFixe;

    public GroupeTiersPayant() {
    }

    public Long getId() {
        return id;
    }

    public GroupeTiersPayant setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public GroupeTiersPayant setName(String name) {
        this.name = name;
        return this;
    }

    public String getAdresse() {
        return adresse;
    }

    public GroupeTiersPayant setAdresse(String adresse) {
        this.adresse = adresse;
        return this;
    }

    public String getTelephone() {
        return telephone;
    }

    public GroupeTiersPayant setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public String getTelephoneFixe() {
        return telephoneFixe;
    }

    public GroupeTiersPayant setTelephoneFixe(String telephoneFixe) {
        this.telephoneFixe = telephoneFixe;
        return this;
    }
}
