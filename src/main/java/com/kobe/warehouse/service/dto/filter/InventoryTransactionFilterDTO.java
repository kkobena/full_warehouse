package com.kobe.warehouse.service.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class InventoryTransactionFilterDTO {
  private Long produitId;
  private String startDate;
  private String endDate;
  private Integer type;
}
