package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.service.utils.NumberUtil;
import java.util.List;

public record DiffereDTO(
    Integer customerId,
    String firstName,
    String lastName,
    Long saleAmount,
    Long paidAmount,
    Long rest,
    List<DiffereItem> differeItems
) {
    public String customerfullName() {
        return String.format("%s %s", firstName, lastName);
    }
    public String formattedPaidAmount() {
        return NumberUtil.formatToString(paidAmount);
    }
    public String formattedSolde() {
        return NumberUtil.formatToString(rest);
    }
    public String sale() {
        return NumberUtil.formatToString(saleAmount);
    }
}
