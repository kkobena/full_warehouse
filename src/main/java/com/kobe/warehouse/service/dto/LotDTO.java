package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

@Getter
public class LotDTO {
  private final Long id;

  private final String numLot;

  private final String receiptRefernce;

  private final Long receiptItemId;

  private final Integer quantity;

  private final Integer ugQuantityReceived = 0;

  private final Integer quantityReceived;

  private final LocalDateTime createdDate;

  private final LocalDate manufacturingDate;

  private final LocalDate expiryDate;

  private final List<LotSoldDTO> lotSolds;

  public LotDTO(Lot lot) {
    id = lot.getId();
    numLot = lot.getNumLot();
    receiptRefernce = lot.getReceiptRefernce();
    receiptItemId = lot.getReceiptItem().getId();
    quantity = lot.getQuantity();
    quantityReceived = lot.getQuantityReceived();
    createdDate = lot.getCreatedDate();
    manufacturingDate = lot.getManufacturingDate();
    expiryDate = lot.getExpiryDate();
    lotSolds =
        !CollectionUtils.isEmpty(lot.getLotSolds())
            ? lot.getLotSolds().stream().map(LotSoldDTO::new).toList()
            : Collections.emptyList();
  }

  @Getter
  private class LotSoldDTO {
    private final Long id;

    private final LocalDateTime createdDate;

    private final String saleReference;

    private final Integer quantity;

    public LotSoldDTO(LotSold lotSold) {
      id = lotSold.getId();
      createdDate = lotSold.getCreatedDate();
      saleReference = lotSold.getSaleLine().getSales().getNumberTransaction();
      quantity = lotSold.getQuantity();
    }
  }
}
