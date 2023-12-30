package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/** A Categorie. */
@Entity
@Table(name = "categorie")
public class Categorie implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code")
  private String code;

  @NotNull
  @Column(name = "libelle", nullable = false, unique = true)
  private String libelle;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

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

  public Categorie libelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public Categorie id(Long id) {
    this.id = id;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Categorie)) {
      return false;
    }
    return id != null && id.equals(((Categorie) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  // prettier-ignore
  @Override
  public String toString() {
    return "Categorie{" + "id=" + getId() + ", libelle='" + getLibelle() + "'" + "}";
  }
}
