package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DeconditionActivityDTO extends AbstractProduitActivity {
  private final TypeDeconditionnement typeDeconditionnement;

  public DeconditionActivityDTO(
      LocalDate dateMvt,
      Integer qtyMvt,
      TypeDeconditionnement typeDeconditionnement,
      LocalDateTime min,
      LocalDateTime max) {
    super(dateMvt, qtyMvt, min, max);
    this.typeDeconditionnement = typeDeconditionnement;
  }

  public TypeDeconditionnement getTypeDeconditionnement() {
    return typeDeconditionnement;
  }
}
