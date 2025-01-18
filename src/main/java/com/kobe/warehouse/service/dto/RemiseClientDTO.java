package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.RemiseClient;

public class RemiseClientDTO extends RemiseDTO {
  private static final long serialVersionUID = -2857904340237832912L;

  public RemiseClientDTO() {}

  public RemiseClientDTO(RemiseClient remise) {
    super(remise);
    this.setTypeLibelle("Remise client");
  }
}
