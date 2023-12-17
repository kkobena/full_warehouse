package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** An authority (a security role) used by Spring Security. */
@Entity
@Table(name = "authority")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Authority implements Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull
  @Size(max = 50)
  @Id
  @Column(length = 50)
  private String name;

  @Column(length = 100)
  private String libelle;

  @JsonIgnore
  @ManyToMany()
  @JoinTable(
      name = "authority_menu",
      joinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "name")},
      inverseJoinColumns = {@JoinColumn(name = "menu_id", referencedColumnName = "id")})
  private Set<Menu> menus = new HashSet<>();

  public Set<Menu> getMenus() {
    return menus;
  }

  public Authority setMenus(Set<Menu> menus) {
    this.menus = menus;
    return this;
  }

  public String getLibelle() {
    return libelle;
  }

  public Authority setLibelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Authority)) {
      return false;
    }
    return Objects.equals(name, ((Authority) o).name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  // prettier-ignore
  @Override
  public String toString() {
    return "Authority{" + "name='" + name + '\'' + "}";
  }
}
