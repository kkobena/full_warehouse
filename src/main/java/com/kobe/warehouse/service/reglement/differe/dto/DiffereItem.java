package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.service.utils.DateUtil;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.time.LocalDateTime;

public record DiffereItem(
    String firstName,
    String lastName,
    String reference,
    int amount,
    int paidAmount,
    int restAmount,
    LocalDateTime mvtDate,
    Long saleId,
    Long customerId
) {
    public String user() {
        return String.format("%s.%s", firstName.charAt(0), lastName);
    }

    public String saleDate() {
        return DateUtil.format(mvtDate);
    }

    public String formattedPaidAmount() {
        return NumberUtil.formatToString(paidAmount);
    }

    public String montantDiffere() {
        return NumberUtil.formatToString(restAmount);
    }

    public String formattedSaleAmount() {
        return NumberUtil.formatToString(amount);
    }
}
