package com.kobe.warehouse.service.dto.projection;

import com.kobe.warehouse.domain.CommandeId;

import java.time.LocalDate;

public interface DeliveryReceiptProjection {

    Integer getId();

    String getReceiptReference();

    String getFournisseurLibelle();

    Integer getReceiptAmount();

    LocalDate getReceiptDate();

    LocalDate getOrderDate();
    Integer getOrderAmount();

    default CommandeId getCommandeId() {
        return new CommandeId(getId(), getOrderDate());
    }

    /*
     private final Integer id;
    private final CommandeId commandeId;

    private final String receiptReference;

    private final LocalDate receiptDate;

    private final int discountAmount;

    private final Integer receiptAmount;

    private final LocalDateTime createdDate;

    private final LocalDateTime modifiedDate;

    private final String createdUser;
    private final Integer fournisseurId;
    private final String fournisseurLibelle;
    private final Integer netAmount;
     */
}
