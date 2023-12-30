package com.kobe.warehouse.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/** A GroupeFournisseur. */
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

  public void setLibelle(String libelle) {
    this.libelle = libelle;
  }

  public GroupeFournisseur libelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public GroupeFournisseur id(Long id) {
    this.id = id;
    return this;
  }

  public String getAddresspostale() {
    return addresspostale;
  }

  public void setAddresspostale(String addresspostale) {
    this.addresspostale = addresspostale;
  }

  public GroupeFournisseur addresspostale(String addresspostale) {
    this.addresspostale = addresspostale;
    return this;
  }

  public String getNumFaxe() {
    return numFaxe;
  }

  public void setNumFaxe(String numFaxe) {
    this.numFaxe = numFaxe;
  }

  public GroupeFournisseur numFaxe(String numFaxe) {
    this.numFaxe = numFaxe;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public GroupeFournisseur email(String email) {
    this.email = email;
    return this;
  }

  public String getTel() {
    return tel;
  }

  public void setTel(String tel) {
    this.tel = tel;
  }

  public GroupeFournisseur tel(String tel) {
    this.tel = tel;
    return this;
  }

  public Integer getOdre() {
    return odre;
  }

  public void setOdre(Integer odre) {
    this.odre = odre;
  }

  public GroupeFournisseur odre(Integer odre) {
    this.odre = odre;
    return this;
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
    return "GroupeFournisseur{"
        + "id="
        + getId()
        + ", libelle='"
        + getLibelle()
        + "'"
        + ", addresspostale='"
        + getAddresspostale()
        + "'"
        + ", numFaxe='"
        + getNumFaxe()
        + "'"
        + ", email='"
        + getEmail()
        + "'"
        + ", tel='"
        + getTel()
        + "'"
        + ", odre="
        + getOdre()
        + "}";
  }
}
