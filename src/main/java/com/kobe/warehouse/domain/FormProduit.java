package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/** A FormProduit. */
@Entity
@Table(name = "form_produit")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FormProduit implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @SequenceGenerator(name = "sequenceGenerator")
  private Long id;

  @NotNull
  @Column(name = "libelle", nullable = false, unique = true)
  private String libelle;

  @OneToMany(mappedBy = "forme")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<Produit> produits = new HashSet<>();

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

  public FormProduit libelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public Set<Produit> getProduits() {
    return produits;
  }

  public void setProduits(Set<Produit> produits) {
    this.produits = produits;
  }

  public FormProduit produits(Set<Produit> produits) {
    this.produits = produits;
    return this;
  }

  // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FormProduit)) {
      return false;
    }
    return id != null && id.equals(((FormProduit) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  // prettier-ignore
  @Override
  public String toString() {
    return "FormProduit{" + "id=" + getId() + ", libelle='" + getLibelle() + "'" + "}";
  }
}
