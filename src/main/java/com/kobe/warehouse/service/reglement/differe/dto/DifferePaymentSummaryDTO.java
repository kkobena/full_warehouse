package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.service.utils.NumberUtil;

public record DifferePaymentSummaryDTO( Long paidAmount, Long solde) {



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
