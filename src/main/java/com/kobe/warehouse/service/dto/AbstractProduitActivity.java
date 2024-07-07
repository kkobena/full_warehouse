package com.kobe.warehouse.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class AbstractProduitActivity {
  private final LocalDate dateMvt;
  private final Integer qtyMvt;
  private final LocalDateTime min;
  private final LocalDateTime max;

  protected AbstractProduitActivity(
      LocalDate dateMvt, Integer qtyMvt, LocalDateTime min, LocalDateTime max) {
    this.dateMvt = dateMvt;
    this.qtyMvt = qtyMvt;
    this.min = min;
    this.max = max;
  }

  public LocalDate getDateMvt() {
    return dateMvt;
  }

  public Integer getQtyMvt() {
    return qtyMvt;
  }

  public LocalDateTime getMin() {
    return min;
  }

  public LocalDateTime getMax() {
    return max;
  }
}
