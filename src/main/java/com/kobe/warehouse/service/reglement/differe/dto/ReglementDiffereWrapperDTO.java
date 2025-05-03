package com.kobe.warehouse.service.reglement.differe.dto;


import com.kobe.warehouse.service.utils.NumberUtil;

import java.util.List;

public record ReglementDiffereWrapperDTO(Long id,  String firstName, String lastName, int paidAmount, int solde,
                                         List<ReglementDiffereDTO>items) {

    public String customerfullName() {
        return String.format("%s %s", firstName, lastName);
    }
    public String formattedPaidAmount() {
        return NumberUtil.formatToString(paidAmount);
    }
    public String formattedSolde() {
        return NumberUtil.formatToString(solde);
    }

}
