package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.service.utils.NumberUtil;

public record DiffereSummary(Long saleAmount, Long paidAmount, Long rest) {
    public String restToString() {
        if (rest == null) {
            return null;
        }
        return NumberUtil.formatToString(rest);
    }

    public String saleAmountToString() {
        if (saleAmount == null) {
            return null;
        }
        return NumberUtil.formatToString(saleAmount);
    }
    public String paidAmountToString() {
        if (paidAmount == null) {
            return null;
        }
        return NumberUtil.formatToString(paidAmount);
    }
}
