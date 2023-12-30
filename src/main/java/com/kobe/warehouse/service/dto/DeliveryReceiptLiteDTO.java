package com.kobe.warehouse.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class DeliveryReceiptLiteDTO {

    private Long id;
    @NotNull
    private Integer receiptAmount;
    private String sequenceBon;
    private String receiptRefernce;
    @NotNull
    private Integer taxAmount;

    private LocalDate receiptDate;
    private String orderReference;

    private Long commandeId;
    private LocalDateTime receiptFullDate;

}
