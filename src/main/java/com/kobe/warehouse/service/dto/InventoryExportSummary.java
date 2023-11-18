package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.service.dto.enumeration.InventoryExportSummaryEnum;
import java.util.Objects;

public class InventoryExportSummary {
  private InventoryExportSummaryEnum name;
  private String libelle;
  private long value;

  public String getLibelle() {
    if (Objects.isNull(libelle) && Objects.nonNull(name)) {
      libelle = name.getValue();
    }

    return libelle;
  }

  public InventoryExportSummary setLibelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public InventoryExportSummaryEnum getName() {
    return name;
  }

  public InventoryExportSummary setName(InventoryExportSummaryEnum name) {
    this.name = name;
    return this;
  }

  public long getValue() {
    return value;
  }

  public InventoryExportSummary setValue(long value) {
    this.value = value;
    return this;
  }
}
