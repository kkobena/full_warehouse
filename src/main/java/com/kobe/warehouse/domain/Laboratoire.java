package com.kobe.warehouse.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/** A Laboratoire. */
@Entity
@Table(name = "laboratoire")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Laboratoire implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @SequenceGenerator(name = "sequenceGenerator")
  private Long id;

  @NotNull
  @Column(name = "libelle", nullable = false, unique = true)
  private String libelle;

  @OneToMany(mappedBy = "laboratoire")
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

  public Laboratoire libelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public Laboratoire id(Long id) {
    this.id = id;
    return this;
  }

  public Set<Produit> getProduits() {
    return produits;
  }

  public void setProduits(Set<Produit> produits) {
    this.produits = produits;
  }

  public Laboratoire produits(Set<Produit> produits) {
    this.produits = produits;
    return this;
  }

  // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Laboratoire)) {
      return false;
    }
    return id != null && id.equals(((Laboratoire) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  // prettier-ignore
  @Override
  public String toString() {
    return "Laboratoire{" + "id=" + getId() + ", libelle='" + getLibelle() + "'" + "}";
  }
}
