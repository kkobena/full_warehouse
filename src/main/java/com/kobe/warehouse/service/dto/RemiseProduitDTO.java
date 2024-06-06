package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.RemiseProduit;

public class RemiseProduitDTO extends RemiseDTO {
  private static final long serialVersionUID = -2857904340237832912L;

  public RemiseProduitDTO() {}

  public RemiseProduitDTO(RemiseProduit remise) {
    super(remise);
    this.setTypeLibelle("Remise produit");
  }
}
