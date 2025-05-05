package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.service.utils.NumberUtil;

import java.math.BigDecimal;

public record DifferePaymentSummaryDTO(Long expectedAmount, Long paidAmount, Long solde) {

    public Long rest() {
        if(expectedAmount == null || paidAmount == null) {
            return null;
        }
        return expectedAmount - paidAmount;
    }
    public String restToString() {
        if(rest() == null) {
            return null;
        }
        return NumberUtil.formatToString(rest());
    }
    public String paidAmountToString() {
        if(paidAmount == null) {
            return null;
        }
        return NumberUtil.formatToString(paidAmount);
    }
    public String formattedSolde() {
        if(solde == null) {
            return null;
        }
        return NumberUtil.formatToString(solde);
    }

}
