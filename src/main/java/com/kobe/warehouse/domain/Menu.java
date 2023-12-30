package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** A Menu. */
@Entity
@Table(
    name = "menu",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class Menu implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "libelle", nullable = false)
  private String libelle;

  @NotNull
  @Column(name = "name", nullable = false, length = 70, unique = true)
  private String name;

  @Column(name = "racine", nullable = false)
  private boolean root;

  @OneToMany(mappedBy = "parent")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<Menu> menus = new HashSet<>();

  @ManyToOne
  @JsonIgnoreProperties(value = "menus", allowSetters = true)
  private Menu parent;

  @Column(name = "enable", nullable = false)
  private boolean enable = true;

  public boolean isRoot() {
    return root;
  }

  public Menu setRoot(boolean root) {
    this.root = root;
    return this;
  }

  public Set<Menu> getMenus() {
    return menus;
  }

  public Menu setMenus(Set<Menu> menus) {
    this.menus = menus;
    return this;
  }

  public Menu getParent() {
    return parent;
  }

  public Menu setParent(Menu parent) {
    this.parent = parent;
    return this;
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

  public Menu libelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Menu name(String name) {
    this.name = name;
    return this;
  }

  public boolean isEnable() {
    return enable;
  }

  public Menu setEnable(boolean enable) {
    this.enable = enable;
    return this;
  }

  // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Menu)) {
      return false;
    }
    return id != null && id.equals(((Menu) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  // prettier-ignore
  @Override
  public String toString() {
    return "Menu{"
        + "id="
        + getId()
        + ", libelle='"
        + getLibelle()
        + "'"
        + ", name='"
        + getName()
        + "'"
        + "}";
  }
}
