package com.kobe.warehouse.domain;

import io.swagger.annotations.ApiModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@ApiModel(description = "not an ignored comment")
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
