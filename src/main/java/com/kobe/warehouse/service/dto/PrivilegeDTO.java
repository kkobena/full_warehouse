package com.kobe.warehouse.service.dto;

import java.util.Objects;

public class PrivilegeDTO {
  private String name;
  private String libelle;

  public PrivilegeDTO() {}

  public PrivilegeDTO(String name, String libelle) {
    this.name = name;
    this.libelle = libelle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PrivilegeDTO that = (PrivilegeDTO) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  public String getName() {
    return name;
  }

  public PrivilegeDTO setName(String name) {
    this.name = name;
    return this;
  }

  public String getLibelle() {
    return libelle;
  }

  public PrivilegeDTO setLibelle(String libelle) {
    this.libelle = libelle;
    return this;
  }
}
