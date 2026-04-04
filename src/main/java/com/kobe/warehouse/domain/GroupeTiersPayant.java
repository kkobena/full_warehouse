package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.OrdreTrisFacture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "groupe_tiers_payant", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupeTiersPayant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    @NotNull
    private String name;

    @Column(name = "adresse", length = 200)
    private String adresse;

    @Column(name = "telephone", length = 15)
    private String telephone;

    @Column(name = "telephone_fixe", length = 15)
    private String telephoneFixe;

    @Enumerated(EnumType.STRING)
    @Column(name = "ordre_tris_facture", length = 20)
    private OrdreTrisFacture ordreTrisFacture;

    @Column(name = "email", length = 100)
    private String email;

    @ColumnDefault("30")
    @Column(name = "delai_reglement")
    private Integer delaiReglement = 30;

    public GroupeTiersPayant() {}

    public Integer getId() {
        return id;
    }

    public GroupeTiersPayant setId(Integer id) {
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

    public OrdreTrisFacture getOrdreTrisFacture() {
        return ordreTrisFacture;
    }

    public GroupeTiersPayant setOrdreTrisFacture(OrdreTrisFacture ordreTrisFacture) {
        this.ordreTrisFacture = ordreTrisFacture;
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

    public String getEmail() {
        return email;
    }

    public GroupeTiersPayant setEmail(String email) {
        this.email = email;
        return this;
    }

    public Integer getDelaiReglement() {
        return delaiReglement;
    }

    public GroupeTiersPayant setDelaiReglement(Integer delaiReglement) {
        this.delaiReglement = delaiReglement;
        return this;
    }
}
