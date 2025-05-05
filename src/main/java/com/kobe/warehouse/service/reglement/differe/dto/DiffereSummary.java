package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.service.utils.NumberUtil;

public record DiffereSummary(Long saleAmout,Long paidAmount,Long rest) {
    public String restToString() {
        if(rest == null) {
            return null;
        }
        return NumberUtil.formatToString(rest);
    }

    public String saleAmoutToString() {
        if(saleAmout == null) {
            return null;
        }
        return NumberUtil.formatToString(saleAmout);
    }
    public String paidAmountToString() {
        if(paidAmount == null) {
            return null;
        }
        return NumberUtil.formatToString(paidAmount);
    }
}
