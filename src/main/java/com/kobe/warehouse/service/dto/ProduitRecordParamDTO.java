package com.kobe.warehouse.service.dto;

import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProduitRecordParamDTO extends VenteRecordParamDTO {
  @Default private OrderBy order = OrderBy.QUANTITY_SOLD;
  private String search;
  private Long produitId;
}
