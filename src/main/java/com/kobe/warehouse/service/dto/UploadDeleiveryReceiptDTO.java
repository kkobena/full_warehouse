package com.kobe.warehouse.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UploadDeleiveryReceiptDTO {

    private CommandeModel model;
    private Long fournisseurId;
    private DeliveryReceiptLiteDTO deliveryReceipt;

}
