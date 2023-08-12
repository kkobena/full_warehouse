package com.kobe.warehouse.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class VenteActivityDTO extends AbstractProduitActivity {
  private final boolean canceled;

  public VenteActivityDTO(LocalDate dateMvt, Integer qtyMvt, boolean canceled, LocalDateTime min,
      LocalDateTime max) {
    super(dateMvt, qtyMvt, min, max);
    this.canceled = canceled;
  }
}
