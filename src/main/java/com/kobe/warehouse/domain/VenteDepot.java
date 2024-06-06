package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import java.io.Serializable;

@Entity
public class VenteDepot extends Sales implements Serializable {
  private static final long serialVersionUID = 1L;
}
