package com.kobe.warehouse.service.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ToString
public class DeliveryReceiptItemLiteDTO {

  @NotNull
  private Long id;
  private Integer quantityUG;
  private Integer quantityReceived;

  private Integer quantityRequested;
  private Integer quantityReturned;
  private List<LotDTO> lots;
  private Integer quantityReceivedTmp;
  private Integer orderUnitPrice;

}
