package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.service.utils.NumberUtil;

public record DifferePaymentSummary(Long rest, Long paidAmount) {
    public String restToString() {
        if(rest == null) {
            return null;
        }
        return NumberUtil.formatToString(rest);
    }
    public String paidAmountToString() {
        if(paidAmount == null) {
            return null;
        }
        return NumberUtil.formatToString(paidAmount);
    }
}
