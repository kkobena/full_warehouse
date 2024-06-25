package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
public class VenteDepot extends Sales implements Serializable {
  private static final long serialVersionUID = 1L;
  @NotNull @ManyToOne private Magasin depot;

  public Magasin getDepot() {
    return depot;
  }

  public VenteDepot setDepot(Magasin depot) {
    this.depot = depot;
    return this;
  }
}
