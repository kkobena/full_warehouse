package com.kobe.warehouse.domain;

import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(
    name = "printer",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})},
    indexes = {@Index(columnList = "name", name = "name_index")})
public class Printer implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  private String address;
  private Boolean defaultPrinter = Boolean.FALSE;

  public Long getId() {
    return id;
  }

  public Printer setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public Printer setName(String name) {
    this.name = name;
    return this;
  }

  public String getAddress() {
    return address;
  }

  public Printer setAddress(String address) {
    this.address = address;
    return this;
  }

  public Boolean getDefaultPrinter() {
    return defaultPrinter;
  }

  public Printer setDefaultPrinter(Boolean defaultPrinter) {
    this.defaultPrinter = defaultPrinter;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Printer printer = (Printer) o;
    return Objects.equals(id, printer.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
